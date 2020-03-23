package service;

import dto.Exposer;
import dto.SeckillExecution;
import exception.RepeatKillException;
import exception.SeckillCloseException;
import exception.SeckillException;
import pojo.Seckill;

import java.util.List;

public interface SeckillService {

    //查询所有秒杀记录
    List<Seckill> getSeckillList();

    //根据Id查询秒杀记录
    Seckill getById(long seckilllId);

    //暴露秒杀地址
    Exposer exposerSeckillUrl(long seckillId);

    //执行秒杀操作,需要判断md5值，如果发现和之前暴露秒杀地址生成的md5值不同，说明被篡改
    //并告知调用方可能抛出的异常
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, SeckillCloseException, RepeatKillException;

    //使用 存储过程 去完成执行秒杀操作
    //这里不再需要抛出之前的三个异常，因为事务已经交由数据库直接管理，
    //不再需要抛出异常去告诉Spring声明式事务进行回滚了
    SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5);
}
