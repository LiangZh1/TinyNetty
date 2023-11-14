package zh1.liang.tiny.netty.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-08-02 16:33
 **/
public abstract class AbstractEventExecutorGroup implements EventExecutorGroup {

    /**
     * @Author: PP-jessica
     * @Description:下面这三个方法也不在该类中，随着代码的进展，代码也会进一步完善
     */
    @Override
    public void shutdownGracefully() {

    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException {

    }

    @Override
    public void execute(Runnable command) {
        next().execute(command);
    }
}
