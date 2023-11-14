package zh1.liang.tiny.netty.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 15:28
 *
 * 定时组件的总接口
 */
public interface Timer {

    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

    Set<Timeout> stop();

}
