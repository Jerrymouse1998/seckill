package exception;

//秒杀关闭异常(秒杀时间结束，秒杀商品为空用于依然使用了Url进行秒杀)
public class SeckillCloseException extends SeckillException {
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
