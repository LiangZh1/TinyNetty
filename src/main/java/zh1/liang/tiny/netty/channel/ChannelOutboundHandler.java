package zh1.liang.tiny.netty.channel;

import java.net.SocketAddress;

/**
 * @author: zhe.liang
 * @create: 2023-09-18 20:57
 */
public interface ChannelOutboundHandler extends ChannelHandler {


    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;


    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                 SocketAddress localAddress, ChannelPromise promise) throws Exception;


    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    void read(ChannelHandlerContext ctx) throws Exception;


    //
    //这里大家会发现在Netty中，这里定义的并不是writeAndFlush方法，而是将这个方法分开成两个方法了
    //这是因为writeAndFlush本身就是两个操作，write方法会把待发送的消息写入Netty自己定义的
    //写缓冲区中，而flush方法会把写缓冲区中的数据刷新到socket缓冲区中
    //这里简单解释一下，后面的课程会引入这些功能，所以这节课就不会再讲解这里了
    //大家默认writeAndFlush方法变成了write和flush方法即可
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;


    void flush(ChannelHandlerContext ctx) throws Exception;

}
