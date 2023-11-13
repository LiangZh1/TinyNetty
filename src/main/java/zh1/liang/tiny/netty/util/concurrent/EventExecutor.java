package zh1.liang.tiny.netty.util.concurrent;

/**
 * @author: zhe.liang
 * @create: 2023-08-01 14:23
 **/
public interface EventExecutor extends EventExecutorGroup{

    @Override
    EventExecutor next();

    EventExecutorGroup parent();

    boolean inEventLoop(Thread thread);
}
