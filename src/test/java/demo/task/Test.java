package demo.task;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-12 12:49
 */
public class Test {


    public static void main(String[] args) throws InterruptedException {
        ScheduledFutureTask fastTask = new ScheduledFutureTask(5000, TimeUnit.NANOSECONDS);

        ScheduledFutureTask slowTask = new ScheduledFutureTask(10000, TimeUnit.NANOSECONDS);

        Comparator<ScheduledFutureTask> SCHEDULED_FUTURE_TASK_COMPARATOR =
                new Comparator<ScheduledFutureTask>() {
                    @Override
                    public int compare(ScheduledFutureTask o1, ScheduledFutureTask o2) {
                        //比较器的实现方法
                        return o1.compareTo(o2);
                    }
                };

        DefaultPriorityQueue<ScheduledFutureTask> queue = new DefaultPriorityQueue(SCHEDULED_FUTURE_TASK_COMPARATOR,16);

        //向定时任务中提交任务
        queue.add(fastTask);
        queue.add(slowTask);

        //启动一个线程去执行定时任务
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //在这里执行具体的任务，从优先级队列中获得定时任务
                    ScheduledFutureTask task = queue.peek();
                    //判断这个定时任务可以执行了吗
                    if(ScheduledFutureTask.nanoTime() <= task.deadlineNanos()){
                        //走到这里说明距程序开始过去的时间小于定时任务的执行时间差，所以要继续等待
                        //但究竟让线程睡多久呢？想一想就会意识到，其实我们只要算出
                        //再过多久过去的时间等于定时任务的时间差就好了，所以，直接让定时任务的时间差
                        //减去当前走过的时间差，得到的就是剩下的时间差，让线程睡这么一会就行
                        long time = task.deadlineNanos() - ScheduledFutureTask.nanoTime();
                        try {
                            Thread.sleep(time);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    //线程睡完之后，就执行定时任务，然后进行下一轮循环，执行下一个定时任务
                    task.run();
                }
            }
        }).start();

    }
}
