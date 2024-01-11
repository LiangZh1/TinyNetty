package demo.threadlocal;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 14:13
 */
public class FastThreadLocalThread extends Thread{

    private final boolean cleanupFastThreadLocals;

    private InternalThreadLocalMap threadLocalMap;

    public FastThreadLocalThread() {
        cleanupFastThreadLocals = false;
    }


    public FastThreadLocalThread(Runnable target, String name) {
        super(FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target, String name) {
        super(group, FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public final InternalThreadLocalMap threadLocalMap() {
        return threadLocalMap;
    }

    //该方法会把InternalThreadLocalMap赋值给该类的threadLocalMap属性
    public final void setThreadLocalMap(InternalThreadLocalMap threadLocalMap) {
        this.threadLocalMap = threadLocalMap;
    }


    public boolean willCleanupFastThreadLocals() {
        return cleanupFastThreadLocals;
    }
}
