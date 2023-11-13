package zh1.liang.tiny.netty.channel;

import zh1.liang.tiny.netty.util.AttributeMap;

import java.net.SocketAddress;

/**
 * @author: zhe.liang
 * @create: 2023-08-03 20:48
 *
 * 这里可以理解一下，Channel可以出发出站，即我们可以主动往某个Socket中写数据
 **/
public interface Channel extends AttributeMap,ChannelOutboundInvoker{

    ChannelId id();

    //该方法很重要，我们都知道，一个selector可以注册多个channel，但是一个channel只能对应
    //一个selector，一个selector对应着一个单线程执行器，所以一个channel就会对应一个单线程执行器
    //该方法就是用来得到该channel对应的单线程执行器
    EventLoop eventLoop();

    Channel parent();


    ChannelConfig config();

    boolean isOpen();

    boolean isRegistered();

    boolean isActive();

    SocketAddress localAddress();

    SocketAddress remoteAddress();

    ChannelFuture closeFuture();

    ChannelPipeline pipeline();

    /**
     * @Author: PP-jessica
     * @Description:终于引入了Unsafe类
     */
    Unsafe unsafe();

    @Override
    Channel read();

    @Override
    Channel flush();

    interface Unsafe{
        SocketAddress localAddress();

        SocketAddress remoteAddress();

        void register(EventLoop eventLoop, ChannelPromise promise);

        void bind(SocketAddress localAddress, ChannelPromise promise);

        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        void disconnect(ChannelPromise promise);

        void close(ChannelPromise promise);

        void closeForcibly();

        void deregister(ChannelPromise promise);

        void beginRead();

        void write(Object msg, ChannelPromise promise);

        void flush();
    }
}
