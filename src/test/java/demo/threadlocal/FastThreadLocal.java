package demo.threadlocal;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.helpers.ThreadLocalMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * @author: zhe.liang
 * @create: 2023-10-18 14:09
 */
@Slf4j
public class FastThreadLocal<V> {

    //始终是0
    private static final int variablesToRemoveIndex = InternalThreadLocalMap.nextVariableIndex();

    //该属性就是决定了fastthreadlocal在threadlocalmap数组中的下标位置
    private final int index;

    public FastThreadLocal() {
        index = InternalThreadLocalMap.nextVariableIndex();
    }


    public static void removeAll(){
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if(threadLocalMap == null){
            return;
        }

        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        if(v != null && v != InternalThreadLocalMap.UNSET){
            //每个线程关联的ThreadLocal都会保存在这个线程私有map的0号位置
            Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
            FastThreadLocal<?>[] variablesToRemoveArray =
                    variablesToRemove.toArray(new FastThreadLocal[0]);
            for (FastThreadLocal<?> tlv: variablesToRemoveArray) {
                tlv.remove(threadLocalMap);
            }
        }

        InternalThreadLocalMap.remove();
    }

    //从数组容器取出对应数据的get方法
    public final V get() {
        //得到存储数据的map
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        return get(threadLocalMap);
    }

    public final V get(InternalThreadLocalMap threadLocalMap) {
        Object v = threadLocalMap.indexedVariable(index);
        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }
        return initialize(threadLocalMap);
    }


    //存储数据到数组容器的set方法
    public final void set(V value) {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        set(threadLocalMap,value);
    }

    public final void set(InternalThreadLocalMap threadLocalMap, V value) {
        if (value != InternalThreadLocalMap.UNSET) {
            setKnownNotUnset(threadLocalMap, value);
        } else {
            remove(threadLocalMap);
        }
    }

    public final boolean isSet() {
        return isSet(InternalThreadLocalMap.getIfSet());
    }


    public final boolean isSet(InternalThreadLocalMap threadLocalMap) {
        return threadLocalMap != null && threadLocalMap.isIndexedVariableSet(index);
    }

    public final void remove() {
        remove(InternalThreadLocalMap.getIfSet());
    }


    public void remove(InternalThreadLocalMap threadLocalMap) {
        if(threadLocalMap == null){
            return;
        }
        Object v = threadLocalMap.removeIndexedVariable(index);
        removeFromVariablesToRemove(threadLocalMap,this);
        if(v != InternalThreadLocalMap.UNSET){
            try {
                //留给用户的回调
                onRemoval((V)v);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

    }

    private static void removeFromVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {
        //根据0下标获得set集合
        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        if (v == InternalThreadLocalMap.UNSET || v == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
        variablesToRemove.remove(variable);
    }




    //设置value到本地map中
    private void setKnownNotUnset(InternalThreadLocalMap threadLocalMap, V value) {
        //设置value到本地map中
        threadLocalMap.setIndexedVariable(index, value);
    }

    /**
     *
     * @param threadLocalMap
     * @param variable
     * 该方法是把该线程引用的fastthreadlocal组成一个set集合，然后放到threadlocalmap数组的0号位置
     */
    @SuppressWarnings("unchecked")
    private static void addToVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {
        //首先得到threadlocalmap数组0号位置的对象
        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        //定义一个set集合
        Set<FastThreadLocal<?>> variablesToRemove;
        if (v == InternalThreadLocalMap.UNSET || v == null) {
            //如果threadlocalmap的0号位置存储的数据为null，那就创建一个set集合
            variablesToRemove = Collections.newSetFromMap(new IdentityHashMap<FastThreadLocal<?>, Boolean>());
            //把InternalThreadLocalMap数组的0号位置设置成set集合
            threadLocalMap.setIndexedVariable(variablesToRemoveIndex, variablesToRemove);
        } else {
            //如果数组的0号位置不为null，就说明已经有set集合了，直接获得即可
            variablesToRemove = (Set<FastThreadLocal<?>>) v;
        }
        //把fastthreadlocal添加到set集合中
        variablesToRemove.add(variable);
    }


    public static int size() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return 0;
        } else {
            return threadLocalMap.size();
        }
    }

    private V initialize(InternalThreadLocalMap threadLocalMap){
        V v = null;

        try {
            v = initialValue();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        threadLocalMap.setIndexedVariable(index, v);
        addToVariablesToRemove(threadLocalMap,this);
        return v;
    }



    protected V initialValue() throws Exception {
        return null;
    }


    protected void onRemoval(@SuppressWarnings("UnusedParameters") V value) throws Exception { }

}
