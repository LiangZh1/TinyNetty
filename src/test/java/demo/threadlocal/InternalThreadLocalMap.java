//package demo.threadlocal;
//
//import java.util.Arrays;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * @author: zhe.liang
// * @create: 2023-10-18 14:02
// */
//public class InternalThreadLocalMap {
//
//    //每创建一个FastThreadLocal对象，该原子对象就自增1
//    static final AtomicInteger nextIndex = new AtomicInteger();
//
//    //存放值的容器
//    Object[] indexedVariables;
//
//    //构造方法
//    private InternalThreadLocalMap() {
//        this.indexedVariables = newIndexedVariableTable();
//    }
//
//
//    //初始化数组，该数组就是在map中存储数据用的
//    private static Object[] newIndexedVariableTable() {
//        Object[] array = new Object[32];
//        Arrays.fill(array, UNSET);
//        return array;
//    }
//
//    public static InternalThreadLocalMap get() {
//        Thread thread = Thread.currentThread();
//        //判断线程是否属于FastThreadLocalThread
//        if (thread instanceof FastThreadLocalThread) {
//            //返回InternalThreadLocalMap
//            return fastGet((FastThreadLocalThread) thread);
//        }
//        //剩下的实现暂且省略
//    }
//
//    //返回InternalThreadLocalMap
//    private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread) {
//        InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();
//        if (threadLocalMap == null) {
//            thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
//        }
//        return threadLocalMap;
//    }
//
//    //取出数组内某个下标位置的元素
//    public Object indexedVariable(int index) {
//        Object[] lookup = indexedVariables;
//        return  lookup[index];
//    }
//
//    //将数组内某个下标位置的数据替换为新的数据
//    public void setIndexedVariable(int index, Object value) {
//        Object[] lookup = indexedVariables;
//        if (index < lookup.length) {
//            lookup[index] = value;
//        } else {
//            //数组扩容方法，该方法就暂不实现了，去源码中学习吧
//            expandIndexedVariableTableAndSet(index, value);
//        }
//    }
//
//
//}
