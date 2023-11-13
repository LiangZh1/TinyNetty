package zh1.liang.tiny.demo.task;

import lombok.SneakyThrows;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-12 16:01
 */
public class AbstractScheduledEventExecutor {

    //定时任务队列需要的比较器
    private static final Comparator<ScheduledFutureTask> SCHEDULED_FUTURE_TASK_COMPARATOR =
            new Comparator<ScheduledFutureTask>() {
                @Override
                public int compare(ScheduledFutureTask o1, ScheduledFutureTask o2) {
                    return o1.compareTo(o2);
                }
            };

    //定时任务队列放在这个类内部
    DefaultPriorityQueue<ScheduledFutureTask> scheduledTaskQueue;

    //内部执行定时任务的线程
    private Thread thread;

    //线程第一次启动的标志，这里为了举例，就忽略并发情况了
    private boolean isRun;

    public AbstractScheduledEventExecutor() {
    }

    //得到距程序开始到现在运行了多久的时间差
    public static long nanoTime() {
        return ScheduledFutureTask.nanoTime();
    }


    //得到定时任务队列的方法
    DefaultPriorityQueue<ScheduledFutureTask> scheduledTaskQueue() {
        //如果优先级任务队列为null，就创建一个再返回。不为null就直接返回优先级任务队列
        if (scheduledTaskQueue == null) {
            //这里把定义好的比较器SCHEDULED_FUTURE_TASK_COMPARATOR传进去了
            scheduledTaskQueue = new DefaultPriorityQueue<ScheduledFutureTask>(
                    //这里的11是队列长度
                    SCHEDULED_FUTURE_TASK_COMPARATOR, 11);
        }
        return scheduledTaskQueue;
    }

    //提交定时任务的方法
    public void schedule(ScheduledFutureTask task) {
        //判空检验就先省略了
        //把定时任务提交到任务队列中
        scheduledTaskQueue().add(task);
        //在这里启动内部线程
        if(!isRun){
            thread = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        //在这里执行具体的任务，从优先级队列中获得定时任务
                        ScheduledFutureTask task = scheduledTaskQueue.peek();
                        //判断这个定时任务可以执行了吗
                        if(ScheduledFutureTask.nanoTime() <= task.deadlineNanos()){
                            //不到执行时间就让线程睡一会
                            long time = task.deadlineNanos() - ScheduledFutureTask.nanoTime();
                            Thread.sleep(time / 1000000);
                        }
                        //线程睡完之后，就执行定时任务，然后进行下一轮循环，执行下一个定时任务
                        task.run();
                        //这里就不添加没有定时任务之后线程是否应该终止的情况了，你想要的手写代码和源码中应有尽有
                    }
                }
            });
            thread.start();
            isRun = true;
        }
    }




}
