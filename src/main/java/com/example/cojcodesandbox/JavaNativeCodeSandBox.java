package com.example.cojcodesandbox;

import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

// 重写父类的方法,就是实现自己的逻辑
/**
 * Java原生代码沙箱
 */
@Component
public class JavaNativeCodeSandBox extends JavaCodeSandboxTemplate {

    // 直接不写也行，也可以直接调用父类的方法
    @Override
    public ExecuteCodeResponse exectueCode(ExecuteCodeRequest exceteCodeRequest) {
        return super.exectueCode(exceteCodeRequest);
    }
}
