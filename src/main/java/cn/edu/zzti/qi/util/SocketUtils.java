package cn.edu.zzti.qi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class SocketUtils {

    private static final Logger logger = LoggerFactory.getLogger(SocketUtils.class);

    public static void write(OutputStream out, byte[]... bytesList) throws IOException {
        out.write(ByteUtils.concat(bytesList));
        out.flush();
    }

    public static byte[] read(InputStream in) throws IOException {
        byte[] data = new byte[0];
        while (true) {
            byte[] buffer = new byte[in.available()];
            int len = in.read(buffer);
            data = ByteUtils.concat(data, ByteUtils.subArray(buffer, 0, len));
            try {
                for(int i = 0; i < 5; ++i) {
                    if(in.available() > 0) break;
                    Thread.sleep((i + 1) * 100);
                }
            } catch (InterruptedException e) {
                logger.error("数据源转换被中断");
            }
            if(in.available() <= 0) break;
        }
        return data;
    }

    public static void transfer(InputStream in, OutputStream out, byte[] byteMap) throws IOException {
        while (true) {
            byte[] buffer = new byte[in.available()];
            int len = in.read(buffer);
            byte[] raw = new byte[len];
            for (int i = 0; i < len; ++i) {
                raw[i] = byteMap[buffer[i] & 0xFF];
            }
            write(out, raw);
            try {
                for(int i = 0; i < 5; ++i) {
                    if(in.available() > 0) break;
                    Thread.sleep((i + 1) * 100);
                }
            } catch (InterruptedException e) {
                logger.error("数据源转换被中断");
            }
            if(in.available() <= 0) break;
        }
    }
}
