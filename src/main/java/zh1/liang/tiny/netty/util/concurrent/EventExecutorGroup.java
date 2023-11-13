package zh1.liang.tiny.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-08-02 13:37
 **/
public interface EventExecutorGroup extends Executor {
    
    EventExecutor next();

    void shutdownGracefully();

    boolean isTerminated();

    void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException;
}
