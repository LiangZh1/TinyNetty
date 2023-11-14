package zh1.liang.tiny.netty.util.concurrent;

import lombok.extern.slf4j.Slf4j;
import zh1.liang.tiny.netty.util.internal.ObjectUtil;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author: zhe.liang
 * @create: 2023-07-25 14:02
 **/
@Slf4j
public abstract class SingleThreadEventExecutor implements EventExecutor {

    //执行器的初始状态，未启动
    private static final int ST_NOT_STARTED = 1;

    //执行器启动后的状态
    private static final int ST_STARTED = 2;

    private volatile int state = ST_NOT_STARTED;

    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");

    private final RejectedExecutionHandler rejectedExecutionHandler;

    private final Queue<Runnable> taskQueue;

    private EventExecutorGroup parent;

    private  boolean addTaskWakesUp;

    private Thread thread;

    //创建线程的执行器
    private Executor executor;

    private volatile boolean interrupted;

    protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor,
                                        boolean addTaskWakesUp, Queue<Runnable> taskQueue,
                                        RejectedExecutionHandler rejectedHandler) {
        //暂时在这里赋值
        this.parent = parent;
        this.addTaskWakesUp = addTaskWakesUp;
        this.executor = executor;
        this.taskQueue = ObjectUtil.checkNotNull(taskQueue, "taskQueue");
        rejectedExecutionHandler = ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler");
    }

    protected Queue<Runnable> newTaskQueue(int maxPendingTasks){
        return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
    }

    @Override
    public void execute(Runnable task) {
        if(task == null){
            throw new NullPointerException("task");
        }
        //把任务提交到任务队列中，这里直接把它提交给队列，是考虑到单线程执行器既要处理IO事件
        //也要执行用户提交的任务，不可能同一时间做两件事。索性就直接先把任务放到队列中。等IO事件处理了
        //再来处理用户任务
        offerTask(task);
        startThread();
    }

    @Override
    public boolean inEventLoop(Thread thread){
        return thread == this.thread;
    }

    protected void runAllTasks() {
        runAllTaskFrom(taskQueue);
    }

    protected void runAllTaskFrom(Queue<Runnable> taskQueue){
        Runnable runnable = pollTaskFrom(taskQueue);
        if(runnable == null){
            return;
        }
        for (;;){
            safeExecute(runnable);
            runnable = pollTaskFrom(taskQueue);
            if (runnable == null) {
                return;
            }
        }
    }

    private Runnable pollTaskFrom(Queue<Runnable> taskQueue){
        return taskQueue.poll();
    }

    protected boolean hasTask(){
        return !taskQueue.isEmpty();
    }

    private void startThread() {
        if(state == ST_STARTED){
            return;
        }
        if (STATE_UPDATER.compareAndSet(this,ST_NOT_STARTED,ST_STARTED)) {
            boolean success = false;
            try {
                doStartThread();
                success = true;
            } finally {
                if (!success) {
                    STATE_UPDATER.compareAndSet(this,ST_STARTED,ST_NOT_STARTED);
                }
            }
        }
    }

    /**
     * @Author: PP-jessica
     * @Description: 中断单线程执行器中的线程
     */
    protected void interruptThread() {
        Thread currentThread = thread;
        if (currentThread == null) {
            interrupted = true;
        } else {
            //中断线程并不是直接让该线程停止运行，而是提供一个中断信号
            //也就是标记，想要停止线程仍需要在运行流程中结合中断标记来判断
            currentThread.interrupt();
        }
    }

    private void doStartThread() {
        //这里的executor是ThreadPerTaskExecutor，runnable -> threadFactory.newThread(command).start()
        //threadFactory中new出来的thread就是单线程线程池中的线程，它会调用nioeventloop中的run方法，无限循环，直到资源被释放
        //代理TaskPerThreadPool
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //Thread.currentThread得到的就是正在执行任务的单线程执行器的线程，这里把它赋值给thread属性十分重要
                //暂时先记住这一点
                thread = Thread.currentThread();
                if (interrupted) {
                    thread.interrupt();
                }
                //线程开始轮询处理IO事件，父类中的关键字this代表的是子类对象，这里调用的是nioeventloop中的run方法
                SingleThreadEventExecutor.this.run();
                log.info("单线程执行器的线程错误结束了！");
            }
        });
    }

    private void offerTask(Runnable task){
        if(task == null){
            throw new NullPointerException("task");
        }
        if (taskQueue.offer(task)) {
            reject(task);
        }
    }

    protected final void reject(Runnable task){
//        rejectedExecutionHandler.rejectedExecution(task,this);
    }

    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            log.error("A task raised an exception. Task: {}", task, t);
        }
    }

    @Override
    public void shutdownGracefully() {

    }

    protected abstract void run();
}
