package com.example.cojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

public class TestSecurityManager {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
//        List<String> strings = FileUtil.readLines("D:\\Project\\Java\\coj-code-sandbox\\src\\main\\resources\\application.yml", "utf-8");
//        System.out.println(strings);
        FileUtil.writeString("aaa", "test", "utf-8");

    }
}
