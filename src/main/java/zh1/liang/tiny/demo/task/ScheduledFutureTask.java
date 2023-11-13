package zh1.liang.tiny.demo.task;

import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-12 13:01
 */
public class ScheduledFutureTask implements Runnable,Comparable<ScheduledFutureTask>{

    //只会加载一次
    private static final long START_TIME = System.nanoTime();


    //当前时间减去开始时间，得到到当前时间为止，距离第一个任务启动已经过去了多少时间
    static long nanoTime() {
        //这里感觉是防止时钟回拨的，不然记一下currentTime + delay不就行了
        return System.nanoTime() - START_TIME;
    }

    static long deadlineNanos(long delay) {
        //如果我的第一个定时任务是在第1秒执行的，也就是开始时间是1秒，现在是3秒，那就是3-1+5等于7
        //也就是说用户提交的定时任务要在距离开始时间过去了7秒的时候执行
        //由此可见，这个时间并不是一个固定的刻度，而是距离第一个定时任务开始过去了多久
        //如果用户提交的一个定时任务要在距离开始时间5秒后执行，但现在System.nanoTime() - START_TIME=6，过去了6秒
        //那这个定时任务的执行时间就过去了，但是还没执行
        long deadlineNanos = nanoTime() + delay;
        return deadlineNanos;
    }


    //这个就是该任务要被执行的时间差
    private long deadlineNanos;

    public ScheduledFutureTask(long delay, TimeUnit unit){
        this.deadlineNanos = deadlineNanos(unit.toNanos(delay));
    }

    //获取定时任务的执行时间
    public long deadlineNanos() {
        return deadlineNanos;
    }


    @Override
    public void run() {
        //当然，这里实际上应该由用户实现逻辑，用户创建自己的ScheduledFutureTask
        //定义自己的定时任务逻辑，但这里为了展示例子，就先写成这样
        System.out.println(Thread.currentThread().getName()+":我现在要去看电影！");
    }


    @Override
    public int compareTo(ScheduledFutureTask task) {
        if(this == task){
            return 0;
        }

        long d = this.deadlineNanos() - task.deadlineNanos();

        if (d < 0) {
            return -1;
        } else {
            return 1;
        }
    }
}
