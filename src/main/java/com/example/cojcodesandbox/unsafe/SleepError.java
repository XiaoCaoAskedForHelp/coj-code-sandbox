package com.example.cojcodesandbox.unsafe;

/**
 * 无线睡眠（阻塞程序执行）
 */
public class SleepError {
    public static void main(String[] args) {
        long ONE_HOUR = 60 * 60 * 1000;
        try {
            Thread.sleep(ONE_HOUR);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("睡眠结束！");
    }
}
