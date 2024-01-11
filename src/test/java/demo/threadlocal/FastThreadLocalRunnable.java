package demo.threadlocal;

import zh1.liang.tiny.netty.util.internal.ObjectUtil;

/**
 * @author: zhe.liang
 * @create: 2024-01-11 21:00
 **/
public class FastThreadLocalRunnable implements Runnable {

    private final Runnable runnable;

    private FastThreadLocalRunnable(Runnable runnable) {
        this.runnable = ObjectUtil.checkNotNull(runnable, "runnable");
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } finally {
            FastThreadLocal.removeAll();
        }
    }

    static Runnable wrap(Runnable runnable) {
        return runnable instanceof FastThreadLocalRunnable ? runnable : new FastThreadLocalRunnable(runnable);
    }
}
