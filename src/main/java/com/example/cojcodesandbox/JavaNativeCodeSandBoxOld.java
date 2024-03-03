package com.example.cojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;
import com.example.cojcodesandbox.model.ExecuteMessage;
import com.example.cojcodesandbox.model.JudgeInfo;
import com.example.cojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandBoxOld implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final String SECURITY_MANAGER_PATH = "D:\\Project\\Java\\coj-code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    // 超时时间
    private static final Long TIME_OUT = 5000L;

    // 黑名单
    private static final List<String> BLACK_LIST = Arrays.asList("Files", "exec");


    private static final WordTree WORD_TREE;

    static {
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(BLACK_LIST);
    }

    public static void main(String[] args) {
        JavaNativeCodeSandBoxOld javaNativeCodeSandBox = new JavaNativeCodeSandBoxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs" + File.separator + GLOBAL_JAVA_CLASS_NAME, StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.exectueCode(executeCodeRequest);
        System.out.println("执行完成" + executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse exectueCode(ExecuteCodeRequest exceteCodeRequest) {
//        System.setSecurityManager(new DefaultSecurityManager());

        String code = exceteCodeRequest.getCode();
        String language = exceteCodeRequest.getLanguage();
        List<String> inputList = exceteCodeRequest.getInputList();

        // 检查用户代码是否包含黑名单
//        if (WORD_TREE.isMatch(code)) {
//            System.out.println("代码中包含黑名单关键字" + WORD_TREE.matchWord(code));
//            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//            executeCodeResponse.setMessage("代码中包含黑名单关键字");
//            executeCodeResponse.setStatus(4);
//            executeCodeResponse.setOutputList(new ArrayList<>());
//            executeCodeResponse.setJudgeInfo(new JudgeInfo());
//            return executeCodeResponse;
//        }

        //1. 把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断文件夹是否存在，不存在则创建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, "UTF-8");

        //2. 编译代码得到class文件
        try {
            String compileCommand = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (IOException e) {
            return handleException(e);
        }

        //3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCommand = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCommand);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时，强制杀死进程");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return handleException(e);
            }
        }

        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        int status = 1;
        // 取用时最大值。便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            if (StrUtil.isNotBlank(executeMessage.getErrorMessge())) {
                executeCodeResponse.setMessage(executeMessage.getErrorMessge());
                // 用户执行的代码出现错误
                status = 3;
                break;
            }
            outputList.add(executeMessage.getMessage());
            if (executeMessage.getTime() > maxTime) {
                maxTime = executeMessage.getTime();
            }
        }
        executeCodeResponse.setOutputList(outputList);
        executeCodeResponse.setStatus(status);
        JudgeInfo judgeInfo = new JudgeInfo();
        // 要借助第三方库来获取内存使用情况
//        judgeInfo.setMemory();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //5. 文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除文件夹" + (del ? "成功" : "失败"));
        }

        System.out.println("执行完成" + executeCodeResponse);
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse handleException(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱执行失败
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
