package demo.mempool.bytebuf;

import demo.mempool.PoolChunk;

/**
 * @author: zhe.liang
 * @create: 2023-11-16 13:56
 **/
public class PooledByteBuf<T> implements ByteBuf{

    //这个属性就是表明了这个PooledByteBuf对象使用的
    //堆外内存是从哪个PoolChunk中分配来的
    protected PoolChunk<T> chunk;

    //把PoolChunk管理的ByteBuffer的引用也赋值给这里
    //这样一来，该类也可以直接使用ByteBuffer的方法来直接操作堆外内存了
    protected T memory;

    //每创建一个PooledByteBuf对象，就要从PoolChunk持有的堆外内存中分配一块内存
    //但是并不直接返回分配到的内存起始地址，而是返回分配到的内存地址距16MB堆外内存起始地址的偏移量
    //这个比较重要，ByteBuf对应的byte数组是同一个，通过不同的偏移量让使用者感觉自己是唯一的
    protected int offset;

    //PooledByteBuf对象分配到的内存大小
    protected int length;

    //给成员变量赋值的方法，该方法会在PoolChunk类中被调用
    void init(PoolChunk<T> chunk,int offset, int length) {
        init0(chunk, offset, length);
    }


    private void init0(PoolChunk<T> chunk,int offset, int length) {
        assert chunk != null;
        this.chunk = chunk;
        memory = chunk.memory;
        this.offset = offset;
        this.length = length;

    }
    //写入字节的方法，这里的index参数，其实很好理解。因为在用户看来他得到的是一个ByteBuf
    //对象，他操作的就是这个对象中的字节容器，这个字节容器对用户来说就是从0到其容量这么大
    //所以用户指定了index为几，就希望把字节消息从字节容器的第几位开始写入
    public void setByte(int index, int value) {
        memory.put(idx(index), (byte) value);
    }


    protected final int idx(int index) {
        return offset + index;
    }



}
