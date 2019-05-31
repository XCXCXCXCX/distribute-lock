package com.xcxcxcxcx.distribute.lock.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public class Test {

    /**
     * JVM中的库存变量
     */
    private int a = 2000;

    /**
     * 业务线程池
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 100,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    private Future<Integer> mock(int num){
        return executor.submit(()->decr(num));
    }

    private synchronized int decr(int num){
        a = a - num;
        return a;
    }

    public static void main(String[] args) {
        //初始化应用
        Test test = new Test();
        List<Future<Integer>> futures = new ArrayList<>();
        //模拟多个用户同时调用接口
        for(int i = 0; i < 1000; i++){
            futures.add(test.mock(1));
        }
        //保证所有请求均处理完成
        for(int i = 0; i < 1000; i++){
            try {
                futures.get(i).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("如果1000个用户调用完，没有出现异常，那么期望值[a=1000]，但实际结果为[a=" + test.a + "]");
    }

}
