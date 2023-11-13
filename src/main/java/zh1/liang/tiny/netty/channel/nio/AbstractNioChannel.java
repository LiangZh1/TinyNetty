package zh1.liang.tiny.netty.channel.nio;

import zh1.liang.tiny.netty.channel.AbstractChannel;
import zh1.liang.tiny.netty.channel.Channel;
import zh1.liang.tiny.netty.channel.ChannelPromise;
import zh1.liang.tiny.netty.channel.EventLoop;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 16:42
 */
public abstract class AbstractNioChannel extends AbstractChannel {

    // 该抽象类是serversocketchannel和socketchannel的公共父类
    protected final SelectableChannel ch;

    // channel要关注的事件
    protected final int readInterestOp;

    // channel注册到selector后返回的key
    protected volatile SelectionKey selectionKey;

    //是否还有未读取的数据
    boolean readPending;

    public AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                // 有异常直接关闭channel
                ch.close();
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
            throw new RuntimeException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    //返回java原生channel的方法
    protected SelectableChannel javaChannel() {
        return ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    protected SelectionKey selectionKey() {
        assert selectionKey != null;
        return selectionKey;
    }

    @Override
    protected boolean isCompatible(EventLoop loop){
        return loop instanceof NioEventLoop;
    }


    @Override
    protected void doRegister() throws Exception {
        //在这里把channel注册到单线程执行器中的selector上,注意这里的第三个参数this，
        // 这意味着channel注册的时候把本身，也就是nio类的channel,当作附件放到key上了，之后会用到这个。
        selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }


    @Override
    protected void doBeginRead() throws Exception {
        final SelectionKey selectionKey = this.selectionKey;
        //检查key是否是有效的
        if (!selectionKey.isValid()) {
            return;
        }
        //还没有设置感兴趣的事件，所以得到的值为0
        final int interestOps = selectionKey.interestOps();
        //interestOps中并不包含readInterestOp
        if ((interestOps & readInterestOp) == 0) {
            //设置channel关注的事件，这里仍然是位运算做加减法
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }

    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;

    protected abstract void doFinishConnect() throws Exception;



    public interface NioUnsafe extends Unsafe {

        SelectableChannel ch();

        void finishConnect();

        void read();

        void forceFlush();
    }

    protected abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe{
        @Override
        public final SelectableChannel ch() {
            return javaChannel();
        }


        @Override
        public final void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            try {
                boolean doConnect = doConnect(remoteAddress, localAddress);
                if (!doConnect) {
                    //这里的代码会搞出一个bug，我会在第六个版本的代码中修正，同时也会给大家讲一下bug是怎么产生的。这个bug只会在收发数据时
                    //体现出来，所以并不会影响我们本节课的测试。我们现在还没有开始收发数据
                    promise.trySuccess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        /**
         * @Author: PP-jessica
         * @Description:暂时不做实现
         */
        @Override
        public final void finishConnect() {}

        @Override
        public final void forceFlush() {}
    }
}
