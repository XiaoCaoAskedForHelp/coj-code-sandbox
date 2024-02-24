package com.example.cojcodesandbox.security;

import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager {
    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何检查");
        System.out.println(perm.toString());
        // 默认就是禁用所有的权限，开启需要注释
        super.checkPermission(perm);
    }
}
