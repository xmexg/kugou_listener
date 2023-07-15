package org.mex.kgListener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * 测试,无用
 */
public class Test {
    public void start() throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream("line 1\nline 2\nline 3\nline 4".getBytes());
            BufferedReader reader = new BufferedReader(new InputStreamReader(bais));
            System.out.println(reader.readLine());
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) > 0)
                sb.append((char)c);
            String remainder = sb.toString();
            System.out.println("`" + remainder + "`");
            InputStream streamWithRemainingLines = new ByteArrayInputStream(remainder.getBytes());
            System.out.println("转换后流:");
            InputStreamReader ISR = new InputStreamReader(streamWithRemainingLines);
            BufferedReader br = new BufferedReader(ISR);
            System.out.println((char)ISR.read());
            char[] ch = new char[5];
            int len;
            // 使用br.read方法读取
            while ((len = br.read(ch)) != -1) {
                System.out.print(new String(ch, 0, len));
            }
//            System.out.println(br.readLine());
//            System.out.println(br.readLine());
//            System.out.println(br.readLine());
//            System.out.println(br.readLine());
//            System.out.println(br.readLine());
//            System.out.println(br.readLine());
    }
}
