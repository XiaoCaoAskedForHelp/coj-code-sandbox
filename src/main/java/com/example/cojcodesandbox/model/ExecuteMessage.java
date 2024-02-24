package com.example.cojcodesandbox.model;

import lombok.Data;

/**
 * 进程执行消息
 */
@Data
public class ExecuteMessage {
    /**
     * 退出码
     */
    private Integer exitCode;
    /**
     * 正常消息
     */
    private String message;
    /**
     * 错误消息
     */
    private String errorMessge;
    /**
     * 执行用时
     */
    private Long time;

    /**
     * 执行内存
     */
    private Long memory;
}
