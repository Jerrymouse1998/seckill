package service.impl;

import dao.SeckillDao;
import dao.SuccessKilledDao;
import dao.cache.RedisDao;
import dto.Exposer;
import dto.SeckillExecution;
import enums.SeckillState;
import exception.RepeatKillException;
import exception.SeckillCloseException;
import exception.SeckillException;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import pojo.Seckill;
import pojo.SuccessKilled;
import service.SeckillService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    //加盐
    private final String salt = "ds@6&^&V$jI#45*%&gw(V%8";

    @Override
    //此方法不是重点，直接写死参数0，4
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    @Override
    public Seckill getById(long seckilllId) {
        return seckillDao.queryById(seckilllId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + '/' + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    @Override
    public Exposer exposerSeckillUrl(long seckillId) {
        //1.先去缓存中找
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            //2.缓存中没有，去数据库找
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null) {
                //3.数据库也没有，直接返回不存在seckillId的信息
                return new Exposer(false, seckillId);
            } else {
                //4.数据库中有，放到缓存中
                redisDao.putSeckill(seckill);
            }
        }
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        //秒杀未开始或已结束
        if (nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    @Override
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillCloseException, SeckillException, RepeatKillException {
        //检查url是否被篡改
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("data of seckill has rewrited!");
        }
        //执行秒杀逻辑：插入秒杀信息+减库存
        //秒杀时间
        Date nowDate = new Date();
        try {
            //插入秒杀信息
            int insertSuccessKilled = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            //重复秒杀
            if (insertSuccessKilled <= 0) {
                //重复秒杀异常
                throw new RepeatKillException("seckill repeated!");
            } else {
                //成功插入秒杀信息，再去执行减库存
                int reduceNumber = seckillDao.reduceNumber(seckillId, nowDate);
                //没有更新秒杀库存信息，秒杀已关闭
                if (reduceNumber <= 0) {
                    //秒杀关闭异常，rollback
                    throw new SeckillCloseException("seckill is closed!");
                } else {
                    //成功秒杀，commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWihtSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillState.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException seckillCloseExcetion) {
            throw seckillCloseExcetion;
        } catch (RepeatKillException repeatKillException) {
            throw repeatKillException;
        } catch (Exception e) {
            //出现异常打印日志
            logger.error(e.getMessage(), e);
            //将所有编译时异常转换成运行时异常，便于Spring声明式事务接受异常对秒杀事务进行回滚
            throw new SeckillException("seckill inner error:" + e.getMessage());
        }
    }

    //使用存储过程的执行秒杀
    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillState.DATA_REWRITE);
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("seckillId", seckillId);
        map.put("userPhone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        try {
            //执行之后result会被赋值
            seckillDao.killByProcedure(map);
            //获取map中的result
            Integer result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled sk = successKilledDao.queryByIdWihtSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillState.SUCCESS, sk);
            } else {
                return new SeckillExecution(seckillId, SeckillState.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillState.INNER_ERROR);
        }
    }
}
