package demo.threadlocal;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 14:02
 */
@Slf4j
public class InternalThreadLocalMap extends UnpaddedInternalThreadLocalMap{
    // Cache line padding (must be public)
    // With CompressedOops enabled, an instance of this class should occupy at least 128 bytes.
    //填充字节用来解决伪共享问题，但是作用不大，新的Netty版本中被废弃了
    public long rp1, rp2, rp3, rp4, rp5, rp6, rp7, rp8, rp9;


    //每创建一个FastThreadLocal对象，该原子对象就自增1
    static final AtomicInteger nextIndex = new AtomicInteger();

    static final Object UNSET = new Object();


    private BitSet cleanerFlags;

    //存放值的容器
    Object[] indexedVariables;

    //构造方法
    private InternalThreadLocalMap() {
        super(newIndexedVariableTable());
    }

    public static InternalThreadLocalMap getIfSet(){
        Thread thread = Thread.currentThread();
        if(thread instanceof FastThreadLocalThread){
            return ((FastThreadLocalThread)thread).threadLocalMap();
        }
        return slowThreadLocalMap.get();
    }

    public ThreadLocalRandom random() {
        ThreadLocalRandom r = random;
        if (r == null) {
            random = r = new ThreadLocalRandom();
        }
        return r;
    }

    public static InternalThreadLocalMap get() {
        Thread thread = Thread.currentThread();
        //判断线程是否属于FastThreadLocalThread
        if (thread instanceof FastThreadLocalThread) {
            //返回InternalThreadLocalMap
            return fastGet((FastThreadLocalThread) thread);
        }else{
            return slowGet();
        }
        //剩下的实现暂且省略
    }

    //返回InternalThreadLocalMap
    private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread) {
        InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();
        if (threadLocalMap == null) {
            thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
        }
        return threadLocalMap;
    }

    private static InternalThreadLocalMap slowGet() {
        ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = UnpaddedInternalThreadLocalMap.slowThreadLocalMap;
        InternalThreadLocalMap ret = slowThreadLocalMap.get();
        if (ret == null) {
            ret = new InternalThreadLocalMap();
            slowThreadLocalMap.set(ret);
        }
        return ret;

    }


    public static void remove(){
        Thread thread = Thread.currentThread();
        if (thread instanceof FastThreadLocalThread) {
            ((FastThreadLocalThread) thread).setThreadLocalMap(null);
        } else {
            slowThreadLocalMap.remove();
        }
    }

    public static void destroy() {
        slowThreadLocalMap.remove();
    }


    public static int nextVariableIndex() {
        int index = nextIndex.getAndIncrement();
        if (index < 0) {
            nextIndex.decrementAndGet();
            throw new IllegalStateException("too many thread-local indexed variables");
        }
        return index;
    }

    public static int lastVariableIndex() {
        return nextIndex.get() - 1;
    }

    /**
     * 得到该map存储元素的个数，这个方法内前面几个判断先别看，因为这里用不到，只看最后一个判断即可
     * 最后一个判断就是取数组里存储元素的个数
     */
    public int size() {
        int count = 0;

        if (futureListenerStackDepth != 0) {
            count ++;
        }
        if (localChannelReaderStackDepth != 0) {
            count ++;
        }
        if (handlerSharableCache != null) {
            count ++;
        }
        if (random != null) {
            count ++;
        }
//        if (typeParameterMatcherGetCache != null) {
//            count ++;
//        }
//        if (typeParameterMatcherFindCache != null) {
//            count ++;
//        }
        if (stringBuilder != null) {
            count ++;
        }
        if (charsetEncoderCache != null) {
            count ++;
        }
        if (charsetDecoderCache != null) {
            count ++;
        }
        if (arrayList != null) {
            count ++;
        }

        for (Object o: indexedVariables) {
            if (o != UNSET) {
                count ++;
            }
        }

        // We should subtract 1 from the count because the first element in 'indexedVariables' is reserved
        // by 'FastThreadLocal' to keep the list of 'FastThreadLocal's to remove on 'FastThreadLocal.removeAll()'.
        return count - 1;
    }

    //初始化数组，该数组就是在map中存储数据用的

    public static Object[] newIndexedVariableTable() {
        Object[] array = new Object[32];
        Arrays.fill(array, UNSET);
        return array;
    }

    //取出数组内某个下标位置的元素
    public Object indexedVariable(int index) {
        Object[] lookup = indexedVariables;
        return lookup[index];
    }

    public Object removeIndexedVariable(int index) {
        Object[] lookup = indexedVariables;
        if (index < lookup.length) {
            Object v = lookup[index];
            lookup[index] = UNSET;
            return v;
        } else {
            return UNSET;
        }
    }

    //将数组内某个下标位置的数据替换为新的数据
    public boolean setIndexedVariable(int index, Object value) {
        Object[] lookup = indexedVariables;
        if (index < lookup.length) {
            Object oldValue = lookup[index];
            lookup[index] = value;
            return oldValue == UNSET;
        } else {
            //数组扩容方法
            expandIndexedVariableTableAndSet(index, value);
            return true;
        }
    }

    private void expandIndexedVariableTableAndSet(int index, Object value) {
        Object[] oldArray = indexedVariables;
        final int oldCapacity = oldArray.length;
        int newCapacity = index;
        newCapacity |= newCapacity >>>  1;
        newCapacity |= newCapacity >>>  2;
        newCapacity |= newCapacity >>>  4;
        newCapacity |= newCapacity >>>  8;
        newCapacity |= newCapacity >>> 16;
        newCapacity ++;

        //扩容数组，把旧的数据拷贝新数组中
        Object[] newArray = Arrays.copyOf(oldArray, newCapacity);
        //新数组扩容的那部分用UNSET赋值
        Arrays.fill(newArray, oldCapacity, newArray.length, UNSET);
        //新数组的index下标的位置赋值为value
        newArray[index] = value;
        //旧数组替换成新数组
        indexedVariables = newArray;
    }

    public boolean isIndexedVariableSet(int index) {
        Object[] lookup = indexedVariables;
        return index < lookup.length && lookup[index] != UNSET;
    }

}
