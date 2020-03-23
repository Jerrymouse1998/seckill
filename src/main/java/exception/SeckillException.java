package exception;

//秒杀业务父异常
// Spring声明式事务只接受运行时异常并进行回滚，如果是编译时异常不会接受并进行回滚。
public class SeckillException extends RuntimeException {
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
