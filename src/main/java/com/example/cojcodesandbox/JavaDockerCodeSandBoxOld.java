package com.example.cojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.example.cojcodesandbox.model.ExecuteCodeRequest;
import com.example.cojcodesandbox.model.ExecuteCodeResponse;
import com.example.cojcodesandbox.model.ExecuteMessage;
import com.example.cojcodesandbox.model.JudgeInfo;
import com.example.cojcodesandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaDockerCodeSandBoxOld implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final boolean FIRST_INIT = true;

    // 超时时间
    private static final Long TIME_OUT = 5000L;

    public static void main(String[] args) {
        JavaDockerCodeSandBoxOld javaNativeCodeSandBox = new JavaDockerCodeSandBoxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode" + File.separator + "simpleComputeArgs" + File.separator + GLOBAL_JAVA_CLASS_NAME, StandardCharsets.UTF_8);
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

        //3. 创建容器，把文件复制到容器内。
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        // 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
        }

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        CreateContainerResponse createContainerResponse = containerCmd
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)   // 打开一个伪终端
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        // 启动容器执行
        dockerClient.startContainerCmd(containerId).exec();

        // docker exec recursing_poitras java -cp /app Main 1 3
        // 执行命令并返回结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0;
            final boolean[] timeout = {true}; // 判断是否超时，若是未超时，那么在onComplete方法中就会设置为false，指示程序未超时
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    super.onComplete();
                    // 未超时就会置位false
                    timeout[0] = false;
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics object) {
                    System.out.println("内存占用:" + object.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(maxMemory[0], object.getMemoryStats().getUsage());
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });

            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeNanos();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setTime(time);
            executeMessage.setErrorMessge(errorMessage[0]);
            executeMessage.setMessage(message[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        int status = 1;
        // 取用时最大值。便于判断是否超时
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            if (StrUtil.isNotBlank(executeMessage.getErrorMessge())) {
                executeCodeResponse.setMessage(executeMessage.getErrorMessge());
                // 用户执行的代码出现错误
                status = 3;
                break;
            }
            outputList.add(executeMessage.getMessage());
            maxTime = Math.max(maxTime, executeMessage.getTime());
            maxMemory = Math.max(maxMemory, executeMessage.getMemory());
        }
        executeCodeResponse.setOutputList(outputList);
        executeCodeResponse.setStatus(status);
        JudgeInfo judgeInfo = new JudgeInfo();
        // 要借助第三方库来获取内存使用情况
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //5. 文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除文件夹" + (del ? "成功" : "失败"));
        }
        // 删除容器
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();

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
