package com.example.cojcodesandbox;

import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱
 */
public interface CodeSandBox {
    /**
     * 执行代码
     *
     * @param exceteCodeRequest
     * @return
     */
    ExecuteCodeResponse exectueCode(ExecuteCodeRequest exceteCodeRequest);
}
