package zh1.liang.tiny.netty.channel;


import zh1.liang.tiny.netty.util.concurrent.EventExecutor;

/**
 * @author: zhe.liang
 * @create: 2023-08-01 14:10
 **/
public interface EventLoop extends EventLoopGroup, EventExecutor {

    EventLoopGroup parent();
}