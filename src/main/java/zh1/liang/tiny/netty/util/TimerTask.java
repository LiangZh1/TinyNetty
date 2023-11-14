package zh1.liang.tiny.netty.util;
/**
 * @author: zhe.liang
 * @create: 2023-10-18 15:29
 */
@FunctionalInterface
public interface TimerTask {
    void run(Timeout timeout) throws Exception;
}
