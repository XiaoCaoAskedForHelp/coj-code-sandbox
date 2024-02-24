package com.example.cojcodesandbox.security;

import java.security.Permission;

public class MySecurityManager extends SecurityManager {
    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
//        super.checkPermission(perm);
    }

    // 检测程序是否有执行某个命令的权限
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec权限异常：" + cmd);
    }

    // 检测程序是否有读取某个文件的权限
    @Override
    public void checkRead(String file) {
//        System.out.println(file);
//        // 通过路径的方式自主控制读的权限，允许读取hutool包下的文件，但是这样比较麻烦
//        if (file.contains("hutool")) {
//            return;
//        }
//        throw new SecurityException("checkRead权限异常：" + file);
    }

    // 检测程序是否有写某个文件的权限
    @Override
    public void checkWrite(String file) {
//        throw new SecurityException("checkWrite权限异常：" + file);
    }

    // 检测程序是否有访问某个网络地址的权限
    @Override
    public void checkConnect(String host, int port) {
//        throw new SecurityException("checkConnect权限异常：" + host + ":" + port);
    }
}
