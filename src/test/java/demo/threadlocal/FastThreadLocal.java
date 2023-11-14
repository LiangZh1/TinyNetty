//package demo.threadlocal;
//
///**
// * @author: zhe.liang
// * @create: 2023-10-18 14:09
// */
//public class FastThreadLocal<V> {
//
//    //该属性就是决定了fastthreadlocal在threadlocalmap数组中的下标位置
//    private final int index;
//
//    //FastThreadLocal构造器，创建的那一刻，threadlocal在map中的下标就已经确定了
//    public FastThreadLocal() {
//        index = InternalThreadLocalMap.nextVariableIndex();
//    }
//
//    //存储数据到数组容器的set方法
//    public final void set(V value) {
//        //得到该线程私有的threadlocalmap
//        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
//        //把值设置进去
//        setKnownNotUnset(threadLocalMap, value);
//    }
//
//    //设置value到本地map中
//    private void setKnownNotUnset(InternalThreadLocalMap threadLocalMap, V value) {
//        //设置value到本地map中
//        threadLocalMap.setIndexedVariable(index, value);
//    }
//
//    //从数组容器取出对应数据的get方法
//    public final V get() {
//        //得到存储数据的map
//        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
//        //根据fastthreadlocal的下标索引获得存储在数组中的数据
//        Object v = threadLocalMap.indexedVariable(index);
//        return (V) v;
//    }
//}
