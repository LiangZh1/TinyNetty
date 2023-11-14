package demo.threadlocal;
/**
 * @author: zhe.liang
 * @create: 2023-10-17 15:27
 */
public class TestJavaTL {

    public static void main(String[] args){
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("嘿嘿");
        String str = threadLocal.get();
        System.out.println(str);
    }
}
