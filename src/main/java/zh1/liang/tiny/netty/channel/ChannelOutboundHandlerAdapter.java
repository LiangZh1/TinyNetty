package zh1.liang.tiny.netty.channel;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.channel.ChannelHandlerMask.Skip;

import java.net.SocketAddress;

/**
 * @author: zhe.liang
 * @create: 2023-10-11 14:32
 *
 * @Description:该类中的所有入出站方法都加上了@Skip注解，Skip为跳过的意思，
 * 那么该类的实现类就可以重写该类的某些方法，去掉@Skip注解，表示该handler对 特定的事件感兴趣
 */
@Slf4j
public class ChannelOutboundHandlerAdapter  extends ChannelHandlerAdapter implements ChannelOutboundHandler {

    @Skip
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }


    @Skip
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }


    @Skip
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }


    @Skip
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.close(promise);
    }


    @Skip
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }


    @Skip
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }


    @Skip
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }


    @Skip
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
