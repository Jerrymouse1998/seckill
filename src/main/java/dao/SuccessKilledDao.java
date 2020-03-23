package dao;

import org.apache.ibatis.annotations.Param;
import pojo.SuccessKilled;

public interface SuccessKilledDao {
    //成功秒杀，插入订单
    int insertSuccessKilled(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);

    //根据Id查成功秒杀信息，并携带返回Seckill对象
    SuccessKilled queryByIdWihtSeckill(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);
}
