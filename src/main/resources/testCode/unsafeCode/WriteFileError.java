import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 写文件错误(植入危险程序)
 */
public class Main {
    public static void main(String[] args) {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        String errorProgram = "java -version 2>&1";
        try {
            Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("写木马程序成功，你完了！");
        }
    }
}
