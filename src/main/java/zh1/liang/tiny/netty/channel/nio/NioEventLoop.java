package zh1.liang.tiny.netty.channel.nio;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.channel.EventLoopGroup;
import zh1.liang.tiny.netty.channel.EventLoopTaskQueueFactory;
import zh1.liang.tiny.netty.channel.SelectStrategy;
import zh1.liang.tiny.netty.channel.SingleThreadEventLoop;
import zh1.liang.tiny.netty.util.concurrent.RejectedExecutionHandler;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-07-31 13:47
 **/
@Slf4j
public class NioEventLoop extends SingleThreadEventLoop {

    private Selector selector;
    private final SelectorProvider selectorProvider;
    private SelectStrategy selectStrategy;
    private int id = 0;
    private static int index = 0;

    private EventLoopGroup workerGroup;


    private ServerSocketChannel serverSocketChannel;

    private SocketChannel socketChannel;


    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                 EventLoopTaskQueueFactory queueFactory){
        super(parent, executor, false, newTaskQueue(queueFactory), newTaskQueue(queueFactory),
                rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        this.selectorProvider = selectorProvider;
        selector = openSelector();
        selectStrategy = strategy;
        log.info("我是" + ++id + "nioeventloop");
        id = index;
        log.info("work" + id);
    }


    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException {

    }


    public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void setWorkerGroup(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }


    public Selector selector() {
        return selector;
    }

    public Selector unwrappedSelector() {
        return selector;
    }


    @Override
    protected void run() {
        while (true) {
            try {
                //没有事件就阻塞在这里
                select();
                //如果走到这里，就说明selector没有阻塞了，可能有IO事件，可能任务队列中有任务
                processSelectedKeys(selector.selectedKeys());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }

    private void select() throws IOException {
        for (; ; ) {
            int selectKeys = selector.select(3000);
            if (selectKeys != 0 || hasTask()) {
                break;
            }
        }
    }

    private void processSelectedKeys(Set<SelectionKey> selectionKeys) throws Exception {
        //采用优化过后的方式处理事件,Netty默认会采用优化过的Selector对就绪事件处理。
        //processSelectedKeysOptimized();
        //未优化过的处理事件方式
        processSelectedKeysPlain(selectionKeys);
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) throws Exception {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object mayChannel = k.attachment();
            i.remove();
            //处理就绪事件
            if (mayChannel instanceof AbstractNioChannel) {
                processSelectedKey(k,(AbstractNioChannel) mayChannel);
            }
            if (!i.hasNext()) {
                break;
            }
        }
    }



    private Selector openSelector() {
        try {
            selector = selectorProvider.openSelector();
            return selector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Queue<Runnable> newTaskQueue(
            EventLoopTaskQueueFactory queueFactory) {
        if (queueFactory == null) {
            return new LinkedBlockingQueue<Runnable>(DEFAULT_MAX_PENDING_TASKS);
        }
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }

    /**
     * @Author: PP-jessica
     * @Description:既然都引入了channel，那么nioeventloop也可以和socketChannel，serverSocketChannel解耦了
     * 这里要重写该方法，现在应该发现了，AbstractNioChannel作为抽象类，既可以调用服务端channel的方法，也可以调用客户端channel的
     * 方法，这就巧妙的把客户端和服务端的channel与nioEventLoop解耦了
     */
    private void processSelectedKey(SelectionKey k,AbstractNioChannel ch) throws Exception {
        try {
            //channel关联的 Unsafe类
            final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
            //得到key感兴趣的事件
            int ops = k.interestOps();
            //如果是连接事件
            if (ops == SelectionKey.OP_CONNECT) {
                //移除连接事件，否则会一直通知，这里实际上是做了个减法。
                ops &= ~SelectionKey.OP_CONNECT;
                //重新把感兴趣的事件注册一下
                k.interestOps(ops);
                //然后再注册客户端channel感兴趣的读事件
                ch.doBeginRead();
            }
            //如果是读事件，不管是客户端还是服务端的，都可以直接调用read方法
            //这时候一定要记清楚，NioSocketChannel和NioServerSocketChannel并不会纠缠
            //用户创建的是哪个channel，这里抽象类调用就是它的方法
            //如果不明白，那么就找到AbstractNioChannel的方法看一看，想一想，虽然那里传入的参数是this，但传入的并不是抽象类本身，想想你创建的
            //是NioSocketChannel还是NioServerSocketChannel，是哪个，传入的就是哪个。只不过在这里被多态赋值给了抽象类
            //创建的是子类对象，但在父类中调用了this，得到的仍然是子类对象
            if (ops ==  SelectionKey.OP_READ) {
                unsafe.read();
            }
            if (ops == SelectionKey.OP_ACCEPT) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            throw new RuntimeException(ignored.getMessage());
        }
    }


}
