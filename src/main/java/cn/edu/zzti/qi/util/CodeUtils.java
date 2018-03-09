package cn.edu.zzti.qi.util;

import java.io.Closeable;
import java.io.IOException;

public final class CodeUtils {

    public static void close(Closeable closeable) {
        try{
            if (null != closeable) {
                closeable.close();
            }
        } catch (IOException e) {
            closeable = null;
        }
    }
}
