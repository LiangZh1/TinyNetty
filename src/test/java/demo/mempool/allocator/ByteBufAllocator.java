package demo.mempool.allocator;

import demo.mempool.bytebuf.ByteBuf;

/**
 * @author: zhe.liang
 * @create: 2023-11-15 13:33
 **/
public interface ByteBufAllocator {

    /**
     * bytebuf对象还是在堆中，但是其内部引用的字节数组在堆外内存
     */
    ByteBuf buffer();

    ByteBuf buffer(int initialCapacity);

    //initialCapacity就是用户需要申请的内存
    //maxCapacity就是可以分配给这个ByteBuf的最大内存，这个最大内存都会使用程序中的默认值
    ByteBuf buffer(int initialCapacity, int maxCapacity);

    //该方法就是直接分配一块堆外内存，交给ByteBuf来包装
    //并且返回该ByteBuf，显然，这个方法会被该接口中的第一个方法调用
    ByteBuf directBuffer();

    ByteBuf directBuffer(int initialCapacity);


    ByteBuf directBuffer(int initialCapacity, int maxCapacity);

}
