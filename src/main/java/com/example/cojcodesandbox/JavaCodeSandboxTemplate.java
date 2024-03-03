package com.example.cojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;
import com.example.cojcodesandbox.model.ExecuteMessage;
import com.example.cojcodesandbox.model.JudgeInfo;
import com.example.cojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 代码沙箱流程模版(模版方法模式)
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandBox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 超时时间
    private static final Long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse exectueCode(ExecuteCodeRequest exceteCodeRequest) {
        String code = exceteCodeRequest.getCode();
        String language = exceteCodeRequest.getLanguage();
        List<String> inputList = exceteCodeRequest.getInputList();

        // 1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        // 2. 编译代码得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        // 4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);
        // 5. 文件清理
        boolean cleared = clearFile(userCodeFile);
        if (!cleared) {
            log.error("文件清理失败,userCodeFilePath:{}", userCodeFile.getAbsolutePath());
        }
        return executeCodeResponse;
    }

    /**
     * 1. 把用户的代码保存为文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
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
        return userCodeFile;
    }

    /**
     * 2. 编译代码得到class文件
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        try {
            String compileCommand = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitCode() != 0) {
                throw new RuntimeException("编译失败");
            }
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 执行代码，得到输出结果
     *
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCommand = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
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
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 收集整理输出结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
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
        return executeCodeResponse;
    }

    /**
     * 5. 清理文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean clearFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 父文件夹不为空
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除文件夹" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6. 获取错误响应
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
