package dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pojo.Seckill;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class RedisDao {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private JedisPool jedisPool;

    private RuntimeSchema schema = RuntimeSchema.createFrom(Seckill.class);

    public RedisDao(String ip, int port) {
        this.jedisPool = new JedisPool(ip, port);
    }

    public Seckill getSeckill(Long seckillId) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "Seckill:" + seckillId;
                //获取key对应的value
                byte[] bytes = jedis.get(key.getBytes());
                if (bytes != null) {
                    //使用schema创建Seckill对象
                    Seckill seckill = (Seckill) schema.newMessage();
                    //使用提供的io工具将bytes反序列化并赋值给seckill
                    ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                    return seckill;
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "Seckill:" + seckill.getSeckillId();
                //序列化对象
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //超时缓存，缓存时间一个小时。成功执行返回OK，否则返回报错信息
                String result = jedis.setex(key.getBytes(), 60 * 60, bytes);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
