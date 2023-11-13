package zh1.liang.tiny.netty.util;

import lombok.Data;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author: zhe.liang
 * @create: 2023-08-14 13:55
 *
 * 数组加链表实现hashMap
 */
public class DefaultAttributeMap implements AttributeMap{

    /**
     * @Author: PP-jessica
     * @Description:原子更新器，这个更新器更新的是map的value的值，在这里，原子更新器是为了解决map添加数据时的并发问题。在hashmap中
     * 哈希桶是普通的数组，而在这个map中，哈希桶为一个原子引用数组。
     */
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> updater =
            AtomicReferenceFieldUpdater.newUpdater(DefaultAttributeMap.class, AtomicReferenceArray.class, "attributes");

    //数组的初始大小为4
    private static final int BUCKET_SIZE = 4;
    //掩码为3，要做位运算求数组下标，这意味着该数组不必扩容
    private static final int MASK = BUCKET_SIZE  - 1;

    //存储数据的数组，并不在这里初始化，而是在第一次向map中添加数据的时候初始化
    @SuppressWarnings("UnusedDeclaration")
    private volatile AtomicReferenceArray<DefaultAttribute<?>> attributes;


    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        if(key == null){
            throw new NullPointerException("empty key");
        }

        AtomicReferenceArray<DefaultAttribute<?>> attributes = this.attributes;

        if(attributes == null){
            attributes = new AtomicReferenceArray<DefaultAttribute<?>>(BUCKET_SIZE);

            if (!updater.compareAndSet(this,null,attributes)) {
                //其他线程更新过了
                attributes = this.attributes;
            }
        }

        int i = index(key);

        DefaultAttribute<?> head = attributes.get(i);


        if(head == null){
            head = new DefaultAttribute();
            DefaultAttribute<T> attr = new DefaultAttribute<>(head, key);
            head.next = attr;
            attr.prev = head;
            if (attributes.compareAndSet(i, null , head)) {
                return attr;
            }else {
                head = attributes.get(i);
            }
        }

        synchronized (head){
            DefaultAttribute<?> curr = head;
            for (;;) {
                //得到当前节点的下一个节点
                DefaultAttribute<?> next = curr.next;
                //如果为null，说明当前节点就是最后一个节点
                if (next == null) {
                    //创建DefaultAttribute对象，封装数据
                    DefaultAttribute<T> attr = new DefaultAttribute<T>(head, key);
                    //当前节点下一个节点为attr
                    curr.next = attr;
                    //attr的上一个节点为当前节点，从这里可以看出netty定义的map中链表采用的是尾插法
                    attr.prev = curr;
                    return attr;
                }
                //如果下一个节点和传入的key相等，并且该节点并没有被删除，说明map中已经存在该数据了，直接返回该数据即可
                if (next.key == key && !next.removed) {
                    return (Attribute<T>) next;
                }
                //把下一个节点赋值为当前节点
                curr = next;
            }
        }
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }

    //该方法是计算要key应该存储在数组的哪个下标位置
    private static int index(AttributeKey<?> key) {
        // 与掩码&运算，数值肯定<=mask 正好是数组下标
        return key.id() & MASK;
    }


    @Data
    private static class DefaultAttribute<T> extends AtomicReference<T> implements Attribute<T>{

        private static final long serialVersionUID = -2661411462200283011L;

        //value在AtomicReference中保存

        public final AttributeKey<T> key;
        //头结点
        private final DefaultAttribute<?> head;

        //前驱和后继结点
        public DefaultAttribute<?> prev;
        public DefaultAttribute<?> next;

        //节点是否被删除了
        public volatile boolean removed;

        DefaultAttribute(DefaultAttribute<?> head, AttributeKey<T> key) {
            this.head = head;
            this.key = key;
        }

        DefaultAttribute() {
            head = this;
            key = null;
        }

        @Override
        public AttributeKey<T> key() {
            return key;
        }

        @Override
        public T setIfAbsent(T value) {
            while (!compareAndSet(null,value)) {
                T old = get();
                if(old != null){
                    return old;
                }
            }
            return null;
        }

        @Override
        public T getAndRemove() {
            removed = true;
            T oldValue = getAndSet(null);
            remove0();
            return oldValue;
        }

        @Override
        public void remove() {
            //表示节点已删除
            removed = true;
            //既然DefaultAttribute都删除了，那么DefaultAttribute中存储的value也该置为null了
            set(null);
            //删除一个节点，重排链表指针
            remove0();
        }

        private void remove0() {
            synchronized (head) {
                if (prev == null) {
                    return;
                }
                prev.next = next;
                if (next != null) {
                    next.prev = prev;
                }
                prev = null;
                next = null;
            }
        }
    }
}
