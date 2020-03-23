package dao;

import org.apache.ibatis.annotations.Param;
import pojo.Seckill;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface SeckillDao {
    //减库存
    int reduceNumber(@Param("seckillId") long seckillId, @Param("killTime") Date killTime);

    //根据ID查
    Seckill queryById(long seckillId);

    //根据参数获取条目
    List<Seckill> queryAll(@Param("offset") int offset, @Param("limit") int limit);

    //调用"execute_seckill"存储过程
    void killByProcedure(Map<String,Object> paramMap);
}
