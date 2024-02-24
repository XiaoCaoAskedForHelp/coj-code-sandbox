package com.example.cojcodesandbox.security;

import java.security.Permission;

/**
 * 禁用所有的权限
 */
public class DenySecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("禁止所有权限" + perm.toString());
    }
}
