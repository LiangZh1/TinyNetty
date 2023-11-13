package zh1.liang.tiny.demo.task;

import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-16 16:06
 */
public class TestExecutor {

    public static void main(String[] args){
        ScheduledFutureTask task = new ScheduledFutureTask(5000, TimeUnit.MILLISECONDS);
        AbstractScheduledEventExecutor scheduled = new AbstractScheduledEventExecutor();
        scheduled.schedule(task);
    }
}
