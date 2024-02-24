package com.example.cojcodesandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 运行其他程序（比如危险木马）
 */
public class RunFileError {
    public static void main(String[] args) throws IOException, InterruptedException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder compileOutputStringBuilder = new StringBuilder();
        // 逐行读取
        String complieOutputLine;
        while ((complieOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(complieOutputLine);
        }
        System.out.println("运行木马程序成功，你完了！");
    }
}
