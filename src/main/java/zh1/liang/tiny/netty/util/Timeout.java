package zh1.liang.tiny.netty.util;
/**
 * @author: zhe.liang
 * @create: 2023-10-18 15:28
 * 对TimerTask的包装
 */

public interface Timeout {

    Timer timer();

    TimerTask task();

    boolean isExpired();

    boolean isCancelled();

    boolean cancel();
}
