package com.example.cojcodesandbox.controller;

import com.example.cojcodesandbox.JavaDockerCodeSandBox;
import com.example.cojcodesandbox.JavaNativeCodeSandBox;
import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {
    // 定义鉴权请求头和秘钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    // 需要定义一个秘钥（如MD5加密）
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    @Resource
    private JavaDockerCodeSandBox javaDockerCodeSandBox;

    @GetMapping("/health")
    public String health() {
        return "I'm healthy!";
    }

    /**
     * 执行代码
     *
     * @param exceteCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse exectueCode(@RequestBody ExecuteCodeRequest exceteCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        // 基本的认证
        if (StringUtils.isBlank(authHeader) || !AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        if (exceteCodeRequest == null || StringUtils.isAnyBlank(exceteCodeRequest.getCode(), exceteCodeRequest.getLanguage())
                || exceteCodeRequest.getInputList() == null || exceteCodeRequest.getInputList().isEmpty()) {
            throw new IllegalArgumentException("exceteCodeRequest请求参数为空");
        }
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.exectueCode(exceteCodeRequest);
        return executeCodeResponse;
    }
}
