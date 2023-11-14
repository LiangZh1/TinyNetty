package demo.IdleHandler;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author: zhe.liang
 * @create: 2023-10-17 12:53
 */
public class TestTimeWhell {

    public final Queue<TimerTask> timeouts = new LinkedBlockingDeque<>();


    //定义一个定时任务，暂且不考虑执行时间
    TimerTask timer = new TimerTask() {
        @Override
        public void run() {
            System.out.println("我是用户定义的定时任务！");
        }
    };


    public static void main(String[] args){
        timeouts.add(timer);
        Thread thread = new Thread(new Worker);


    }
    //再实现Runnable
    private final class Worker implements Runnable {
        @Override
        public void run() {
            //上面只提交了一个定时任务到队列中
            //所以这里就先不用循环了
            TimerTask task = timeouts.poll();
            task.run();
        }
    }
}
