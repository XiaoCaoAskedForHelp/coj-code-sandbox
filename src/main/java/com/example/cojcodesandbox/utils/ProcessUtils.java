package com.example.cojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.example.cojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 运行进程并获取消息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待编译完成
            int exitValue = runProcess.waitFor();
            executeMessage.setExitCode(exitValue);
            // 判断编译是否成功
            if (exitValue == 0) {
                System.out.println(opName + "成功");
            } else {
                System.out.println(opName + "失败，错误码：" + exitValue);
                // 分批获取进程的错误输出流
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String complieErrorLine;
                while ((complieErrorLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(complieErrorLine);
                }
                executeMessage.setErrorMessge(StringUtils.join(errorOutputStrList, "\n"));
            }
            // 分批获取进程的正常输出流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            List<String> outputStrList = new ArrayList<>();
            // 逐行读取
            String complieOutputLine;
            while ((complieOutputLine = bufferedReader.readLine()) != null) {
                outputStrList.add(complieOutputLine);
            }
            executeMessage.setMessage(StringUtils.join(outputStrList, "\n"));

            stopWatch.stop();
            executeMessage.setTime(stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 运行交互式进程并获取消息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runInterProcessAndGetMessage(Process runProcess, String opName, String inputArgs) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            InputStream inputStream = runProcess.getInputStream();
            OutputStream outputStream = runProcess.getOutputStream();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = inputArgs.split(" ");
            outputStreamWriter.write(StrUtil.join("\n", s) + "\n");
            outputStreamWriter.flush();
            // 分批获取进程的正常输出流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String complieOutputLine;
            while ((complieOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(complieOutputLine);
            }
//            System.out.println(compileOutputStringBuilder.toString());
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 资源释放
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
