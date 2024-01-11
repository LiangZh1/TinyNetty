package demo.threadlocal;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: zhe.liang
 * @create: 2024-01-11 20:34
 *
 * ThreadLocal的父类，包装了一些属性
 **/
public class UnpaddedInternalThreadLocalMap {

    //如果使用的线程不是fastthreadlocalthread，那就返回一个原生的ThreadLocal，原生的ThreadLocal可以得到原生的ThreadLocalMap
    static final ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = new ThreadLocal<>();

    static final AtomicInteger nextIndex = new AtomicInteger();

    Object[] indexedVariables;

    int futureListenerStackDepth;
    int localChannelReaderStackDepth;
    Map<Class<?>, Boolean> handlerSharableCache;

    ThreadLocalRandom random;

    StringBuilder stringBuilder;
    Map<Charset, CharsetEncoder> charsetEncoderCache;
    Map<Charset, CharsetDecoder> charsetDecoderCache;

    ArrayList<Object> arrayList;


    UnpaddedInternalThreadLocalMap(Object[] vales) {
        this.indexedVariables = vales;
    }


}
