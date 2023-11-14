package demo.IdleHandler;

import lombok.SneakyThrows;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-17 13:08
 *
 *
 * Netty 保留了普通的任务队列，并且把用户提交的定时任务先存放到普通任务队列中，当然，这一部分仍然是主线程来执行的。
 * 但是将定时任务从普通任务队列中转移到时间轮的数组中，却是让执行时间轮中定时任务的线程来处理的。
 * 并且是在每次执行下一个刻度对应的定时任务前，把即将要执行的定时任务从 timeouts 队列中转移到时间轮数组容器中
 *
 * 可以避免并发问题
 */
public class TestWheelTimer {

    //数组的每一个位置存放的是HashedWheelBucket类型的双向链表
    //HashedWheelBucket这个类型我们刚刚讲解了
    private final HashedWheelBucket[] wheel;

    //一个时间刻度代表的时间
    private final long tickDuration;

    //掩码，计算定时任务要存入的数组的下标
    //这个掩码其实就是数组的长度减一，计算定时任务存储的数组下标时要取余数
    //但是和数组的长度减一，然后做与运算，性能更好，所以弄了一个掩码出来
    //但这就要求时间轮数组的长度必须是2的n次方
    private final int mask;

    //时间轮的启动时间
    private volatile long startTime;

    //这个属性也很重要，是用来精确计算时间轮启动时间的
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    //该时间轮要执行的runable，时间轮是单线程的，单线程执行的是该runnable，其中的run方法就是时间轮的核心逻辑
    private final Worker worker = new Worker();

    //这个属性就是时间轮中的线程组件，真正执行时间轮定时任务的线程
    //workerThread 单线程用于处理时间轮中所有的定时任务，这意味着定时任务不能有较大的阻塞和耗时，不然就会影响定时任务执行的准时性和有效性。
    private final Thread workerThread;

    //用来暂时存放用户提交的定时任务的队列，之后这些定时任务会被时间轮中的线程转移到时间轮数组中
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingDeque<>();

    //时间轮线程是否启动的标志
    private boolean isRun;

    /**
     * @param tickDuration 一个刻度代表的时间
     * @param unit
     * @param ticksPerWheel 整个轮子有多少个间隔
     */
    public TestWheelTimer (long tickDuration, TimeUnit unit, int ticksPerWheel){
        //创建时间轮数组，ticksPerWheel是数组的容量，也就是刻度盘的总刻度
        wheel = createWheel(ticksPerWheel);
        //掩码，计算定时任务要存放的数组下标
        mask = wheel.length - 1;
        //时间换算成纳秒
        long duration = unit.toNanos(tickDuration);
        //给时间间隔赋值
        this.tickDuration = duration;
        //创建工作线程，注意，这里只是创建，还没有启动时间轮线程
        workerThread = new Thread(worker);
    }



    //接下来是创建时间轮数组的方法，其实就是传进去一个整数，当作时间轮数组的容量
    //也就是表盘总共的刻度
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        //把时间轮数组长度设定到2的次方
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        //初始化每一个位置的bucket
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i ++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    //长度设置到2的N次方的方法
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }

    public HashedWheelTimeout newTimeout(TimerTask task, long delay, TimeUnit unit){
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        //启动工作线程，并且确保只启动一次，这里面会涉及线程的等待和唤醒
        start();

        //计算该定时任务的执行时间，startTime是worker线程的开始时间。以后所有添加进来的任务的执行时间，都是根据这个开始时间做的对比
        long deadline = System.nanoTime() - startTime + unit.toNanos(delay);
        //把提交的任务封装进一个HashedWheelTimeout中。这时候任务的执行时间也都计算好了
        HashedWheelTimeout timeout = new HashedWheelTimeout(task, deadline);
        //将定时任务添加到普通任务队列中
        timeouts.add(timeout);
        return timeout;
    }

    public void start(){
        if (!isRun) {
            workerThread.start();
            isRun = true;
        }
        while (startTime == 0){
            try {
                startTimeInitialized.await();
            } catch (InterruptedException e) {

            }
        }
    }

    private final class Worker implements Runnable {
        private long tick;

        @Override
        public void run() {
            startTime = System.nanoTime();
            startTimeInitialized.countDown();

            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    // 获取要执行的定时任务的那个数组下标。就是让指针当前的刻度和掩码做位运算
                    int idx = (int) (tick & mask);
                    // 上面已经得到了要执行的定时任务的数组下标，这里就可以得到该bucket，而这个bucket就是定时任务的一个双向链表
                    // 链表中的每个节点都是一个定时任务
                    HashedWheelBucket bucket = wheel[idx];
                    // 在真正执行定时任务之前，把即将被执行的任务从普通任务队列中放到时间轮的数组当中
                    transferTimeoutsToBuckets();
                    // 执行定时任务
                    bucket.expireTimeouts(deadline);
                    // 指针已经移动了，所以加1
                    tick++;
                }
            } while (true);
        }

        private long waitForNextTick(){
            //直接执行了多少tick
            long deadline = tickDuration * (tick + 1);

            for(;;){
                final long currentTime = System.nanoTime() - startTime;
                //这里加上999999的是因为除法只会取整数部分，为了保证任务不被提前执行，加上999999后就能够向上取整1ms。
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                if (sleepTimeMs <= 0) {
                    //返回work线程已经run的时间
                    return currentTime;
                }
                try {
                    //走到这里就意味着还没到执行时间，需要睡一会才行
                    //这里大家应该也能看出来，在Netty中的时间轮，其实是先走过时间，然后再执行定时任务
                    //比如有个定时任务是立刻执行的，就当是0秒执行，这个定时任务肯定会被放到数组的0下标
                    //但是时间轮启动后，得让时间走到1的刻度，才会把数组0号位对应的定时任务执行了
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {

                }

            }
        }

        private void transferTimeoutsToBuckets(){
            for(int i = 0; i < 100000; i++) {
                HashedWheelTimeout task = timeouts.poll();
                if(task == null){
                    break;
                }
                //tickDuration就是时间刻度，一个刻度可能是1秒，也可能是2秒等等
                //计算程序开始运行算起，要经过多少个刻度才能执行到这个任务
                long calculated = task.deadline / tickDuration;
                //位运算计算出该定时任务应该放在的数组下标
                //如果要经过2个刻度，那就让这个刻度直接和掩码做位运算，得到的就是
                //这个定时任务要存放到数组中的位置
                int stopIndex = (int) (calculated & mask);
                //得到数组下标中的bucket节点
                HashedWheelBucket bucket = wheel[stopIndex];
                //把定时任务添加到链表之中
                bucket.addTimeout(task);
            }
        }
    }


    /**
     * 对task的包装
     */
    private static final class HashedWheelTimeout {

        private final TimerTask task;

        //这个就是定时任务的执行时间，就是定时任务要执行的时间和程序还是执行的时间的时间差
        //也就是从程序开始启动，经过多少时间之后定时任务应该执行了
        public final long deadline;

        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        //定时任务所在的时间轮数组的位置，这个属性马上就会讲解
        HashedWheelBucket bucket;

        //接下来是构造方法
        HashedWheelTimeout(TimerTask task, long deadline) {
            this.task = task;
            this.deadline = deadline;
        }

        //执行定时任务的方法
        @SneakyThrows
        public void expire() {
            task.run();
        }

    }


    /**
     * 时间轮中的Bucket
     */
    private static class HashedWheelBucket {

        //HashedWheelTimeout链表的头节点和尾节点
        private HashedWheelTimeout head;
        private HashedWheelTimeout tail;

        //添加HashedWheelTimeout节点的方法
        public void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                //这里虽然是头尾节点，但实际上添加第一个节点的时候，头节点和为节点和添加的节点就变成了同一个节点
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        public void expireTimeouts(long deadline){
            //获取链表的头节点，注意啊，这时候已经定位到了具体的bucket了
            HashedWheelTimeout timeout = head;
            while(timeout != null){
                HashedWheelTimeout next = timeout.next;
                if(timeout.deadline <= deadline){
                    //执行定时任务
                    timeout.expire();
                }else {
                    throw new IllegalStateException(String.format(
                            "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                }
                timeout = next;
            }
        }

    }


}
