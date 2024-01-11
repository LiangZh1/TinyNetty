package demo.mempool.allocator;

import demo.mempool.bytebuf.ByteBuf;

import static zh1.liang.tiny.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author: zhe.liang
 * @create: 2023-11-15 13:44
 **/
public abstract class AbstractByteBufAllocator implements ByteBufAllocator{
    //默认分配的内存的初始大小
    static final int DEFAULT_INITIAL_CAPACITY = 256;

    //默认的分配内存的最大值
    static final int DEFAULT_MAX_CAPACITY = Integer.MAX_VALUE;

    //默认使用堆外内存
    private final boolean directByDefault = true;

    private final ByteBuf emptyBuf = new ByteBuf();

    public AbstractByteBufAllocator(boolean preferDirect) {
    }

    @Override
    public ByteBuf buffer() {
        if (directByDefault) {
            return directBuffer();
        }
        return null;
    }

    @Override
    public ByteBuf buffer(int initialCapacity) {
        if (directByDefault) {
            return directBuffer(initialCapacity);
        }
        return null;
    }

    @Override
    public ByteBuf buffer(int initialCapacity, int maxCapacity) {
        if (directByDefault) {
            return directBuffer(initialCapacity, maxCapacity);
        }
        return null;
    }

    @Override
    public ByteBuf directBuffer() {
        return directBuffer(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity) {
        return directBuffer(initialCapacity, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
        if (initialCapacity == 0 && maxCapacity == 0) {
            return emptyBuf;
        }
        validate(initialCapacity, maxCapacity);
        return newDirectBuffer(initialCapacity, maxCapacity);
    }

    //该方法用来创建堆内存的对象
    protected abstract ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity);

    //该方法用来创建堆内存的对象，在我们的课程中不会使用这个方法
    protected abstract ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity);

    private static void validate(int initialCapacity, int maxCapacity) {
        checkPositiveOrZero(initialCapacity, "initialCapacity");
        if (initialCapacity > maxCapacity) {
            throw new IllegalArgumentException(String.format(
                    "initialCapacity: %d (expected: not greater than maxCapacity(%d)",
                    initialCapacity, maxCapacity));
        }
    }
}
