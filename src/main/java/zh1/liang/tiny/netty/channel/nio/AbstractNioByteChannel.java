package zh1.liang.tiny.netty.channel.nio;

import zh1.liang.tiny.netty.channel.Channel;
import zh1.liang.tiny.netty.channel.ChannelPipeline;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 19:59
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel{

    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }



    //真正读取消息的方法，留给子类去实现
    protected abstract int doReadBytes(ByteBuffer buf) throws Exception;

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    protected void doWrite(Object masg) throws Exception {

    }

    protected class NioByteUnsafe extends AbstractNioUnsafe {
        @Override
        public void read() {
            //得到ChannelPipeline
            final ChannelPipeline pipeline = pipeline();
            //暂时用最原始的方法处理
            ByteBuffer byteBuf = ByteBuffer.allocate(1024);
            try {
                int bytes = doReadBytes(byteBuf);
                //源码中并没有下面这个判断分支，这里这么写是为了再客户端channel关闭的时候，服务端可以不报错。后面我们会逐步完善。
                if (bytes == -1) {
                    return;
                }
                //把数据向后面的handler传递做处理
                pipeline.fireChannelRead(byteBuf);
                //新增加一个方法，这个方法是为了配合心跳检测使用的，暂时写成这样，等后面重构read方法时候会再次讲到该方法
                pipeline.fireChannelReadComplete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
