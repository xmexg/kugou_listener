package org.mex.kgListener;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 数据包处理,无用
 */
public class Handle {

    public InputStream song2free(InputStream input) {
        System.out.println("handle");
        InputStream inputStream = input;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
//                System.out.println(line);
            }
            String result = stringBuilder.toString();
            // 在这里使用 result 进行你的逻辑操作
//            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }
}
