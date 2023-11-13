package zh1.liang.tiny.netty.channel;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.util.DefaultAttributeMap;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author: zhe.liang
 * @create: 2023-08-06 16:32
 */
@Slf4j
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel{

    /**
     * @Description:当创建的是客户端channel时，parent为serversocketchannel
     * 如果创建的为服务端channel，parent则为null
     */
    private final Channel parent;

    private final ChannelId id;

    /**
     * @Description:加入unsafe属性了
     */
    private final Unsafe unsafe;

    private final DefaultChannelPipeline pipeline;


    /**
     * 看名字也可以猜出，这个future是在channel关闭的时候使用的，是一个静态内部类
     */
    private final CloseFuture closeFuture = new CloseFuture(this);



    private volatile SocketAddress localAddress;

    private volatile SocketAddress remoteAddress;

    private Throwable initialCloseCause;

    //每一个channel都需要绑定在一个eventLoop
    private volatile EventLoop eventLoop;

    private volatile boolean registered;

    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        unsafe = newUnsafe();
        id = newId();
        pipeline = newChannelPipeline();
    }

    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        unsafe = newUnsafe();
        this.id = id;
        pipeline = newChannelPipeline();
    }


    protected abstract Unsafe newUnsafe();

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public final ChannelId id() {
        return id;
    }


    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public Channel parent() {
        return parent;
    }



    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public Channel flush() {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }
    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        DefaultChannelPromise promise = new DefaultChannelPromise(this);
        unsafe.write(msg,promise);
        return promise;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }
    //在源码中config类并不在这里实现，而是放在了niosocketchannel和nioserversocketchannel
    //中分别实现，这么做是因为客户端和服务端的配置并相同，所以要分别作处理，这也是定义公共接口
    //子类各自实现的一种体现
//    @Override
//    public ChannelConfig config() {
//        return null;
//    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public Channel read() {
        unsafe.beginRead();
        return this;
    }

    /**
     * @Author: PP-jessica
     * @Description:得到本地地址
     */
    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }
        return localAddress;
    }

    /**
     * @Author: PP-jessica
     * @Description:得到远程地址
     */
    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }
        return remoteAddress;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        unsafe.connect(remoteAddress,localAddress,promise);
        return promise;
    }

    protected DefaultChannelPipeline newChannelPipeline() {
        //把创建出的channel传入DefaultChannelPipeline；
        return new DefaultChannelPipeline(this);
    }

    //给channel注册感兴趣事件
    public final void beginRead() {
        //如果是服务端的channel，这里仍然可能为false
        //那么真正注册读事件的时机，就成了绑定端口号成功之后
        if (!isActive()) {
            return;
        }
        try {
            doBeginRead();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //在很多框架中，有一个规定，那就是真正干事的方法都是do开头的
    protected void doRegister() throws Exception{

    }

    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    /**
     * @Author: PP-jessica
     * @Description:确保channel是打开的
     */
    protected final boolean ensureOpen(ChannelPromise promise) {
        if (isOpen()) {
            return true;
        }
        safeSetFailure(promise, newClosedChannelException(initialCloseCause));
        return false;
    }


    protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
        if (!promise.tryFailure(cause)) {
            throw new RuntimeException(cause);
        }
    }

    private ClosedChannelException newClosedChannelException(Throwable cause) {
        ClosedChannelException exception = new ClosedChannelException();
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }
    protected final void safeSetSuccess(ChannelPromise promise) {
        if (!promise.trySuccess()) {
            System.out.println("Failed to mark a promise as success because it is done already: "+promise);
        }
    }
    protected abstract void doBeginRead() throws Exception;

    protected abstract boolean isCompatible(EventLoop loop);

    @Override
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        //TODO
        return pipeline.bind(localAddress,promise);
    }

    protected abstract void doBind(SocketAddress localAddress) throws Exception;


    protected abstract SocketAddress localAddress0();

    protected abstract SocketAddress remoteAddress0();

    protected abstract void doClose() throws Exception;

    protected abstract void doWrite(Object msg) throws Exception;

    protected abstract class AbstractUnsafe implements Unsafe{
        private void assertEventLoop() {
            assert !registered || eventLoop.inEventLoop(Thread.currentThread());
        }

        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return remoteAddress0();
        }


        @Override
        public final void register(EventLoop eventLoop,ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }

            //检查channel是否注册过，注册过就手动设置promise失败
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }

            //判断当前使用的执行器是否为NioEventLoop，如果不是手动设置失败
            if (!isCompatible(eventLoop)) {
                promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            //在这里就把channel绑定的单线程执行器属性给赋值了
            AbstractChannel.this.eventLoop = eventLoop;
            //接下来就是之前写过的常规逻辑
            if (eventLoop.inEventLoop(Thread.currentThread())) {
                register0(promise);
            } else {
                //如果调用该放的线程不是netty的线程，就封装成任务由线程执行器来执行
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            //为什么这里的register0方法可以不需要参数了？
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    System.out.println(t.getMessage());
                    //该方法先不做实现，等引入unsafe之后会实现
                    //closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

        private void register0(ChannelPromise promise) {
            try {
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }

                //真正的注册方法
                doRegister();
                registered = true;
                //在这里给channel注册感兴趣事件
                //把成功状态赋值给promise，这样它可以通知回调函数执行
                //我们在之前注册时候，把bind也放在了回调函数中
                safeSetSuccess(promise);
                beginRead();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            //这里一定为false，因为channel还未绑定端口号，肯定不是激活状态
            boolean wasActive = isActive();
            try {
                //这里就调用了真正绑定端口号的方法，是nio原生的方法
                doBind(localAddress);
            } catch (Exception e) {
                safeSetFailure(promise, e);
            }
            //这时候一定为true了
            if (!wasActive && isActive()) {
                //然后会向单线程执行器中提交任务，任务重会执行ChannelPipeline中每一个节点中handler的ChannelActive方法
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //端口号绑定成功后，会触发一个入站事件，会在pipeline链表中
                        //调用所有入站处理器的channelActive方法
                        pipeline.fireChannelActive();
                    }
                });
            }
            //设置promise成功状态，通知用户绑定端口号成功了
            safeSetSuccess(promise);
        }

        /**
         * @Author: PP-jessica
         * @Description:暂时不做实现，接下来的一些方法都不做实现，等之后讲到了再实现
         */
        @Override
        public final void disconnect(final ChannelPromise promise) {}

        @Override
        public final void close(final ChannelPromise promise) {}

        /**
         * @Author: PP-jessica
         * @Description:强制关闭channel
         */
        @Override
        public final void closeForcibly() {
            assertEventLoop();

            try {
                doClose();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        @Override
        public final void deregister(final ChannelPromise promise) {}

        @Override
        public final void beginRead() {
            assertEventLoop();
            //如果是服务端的channel，这里仍然可能为false
            //那么真正注册读事件的时机，就成了绑定端口号成功之后
            if (!isActive()) {
                return;
            }
            try {
                doBeginRead();
            } catch (final Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        @Override
        public final void write(Object msg, ChannelPromise promise) {}

        @Override
        public final void flush() {}

        /**
         * @Author: PP-jessica
         * @Description:确保channel是打开的
         */
        protected final boolean ensureOpen(ChannelPromise promise) {
            if (isOpen()) {
                return true;
            }
            safeSetFailure(promise, newClosedChannelException(initialCloseCause));
            return false;
        }

        protected final void safeSetSuccess(ChannelPromise promise) {
            if (!promise.trySuccess()) {
                System.out.println("Failed to mark a promise as success because it is done already: "+promise);
            }
        }

        protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
            if (!promise.tryFailure(cause)) {
                throw new RuntimeException(cause);
            }
        }

        private void invokeLater(Runnable task) {
            try {
                eventLoop().execute(task);
            } catch (RejectedExecutionException e) {
                log.warn("Can't invoke task later as EventLoop rejected it", e);
            }
        }
    }


    static final class CloseFuture extends DefaultChannelPromise {

        CloseFuture(AbstractChannel ch) {
            super(ch);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }
    }
}
