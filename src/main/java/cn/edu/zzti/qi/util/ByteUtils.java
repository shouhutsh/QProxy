package cn.edu.zzti.qi.util;

import cn.edu.zzti.qi.exception.BizException;

public final class ByteUtils {

    public static byte[] concat(byte[]... bytesList) {
        if (null == bytesList) return null;
        if (1 == bytesList.length) return bytesList[0];

        int size = 0;
        for (byte[] bytes : bytesList) {
            if(null == bytes) return null;
            size += bytes.length;
        }

        int temp = 0;
        byte[] target = new byte[size];
        for (byte[] bytes : bytesList) {
            System.arraycopy(bytes, 0, target, temp, bytes.length);
            temp += bytes.length;
        }
        return target;
    }

    public static byte[] subArray(byte[] source, int off, int size) {
        if (null != source && off >= 0 && size > 0 && off + size < source.length) {
            byte[] target = new byte[size - off];
            System.arraycopy(source, off, target, 0, size);
            return target;
        }
        return source;
    }

    public static byte[] transferShort(short s) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((s >>> 8) & 0xFF);
        bytes[1] = (byte) ((s) & 0xFF);
        return bytes;
    }

    public static short transferShort(byte[] bytes) {
        if (null != bytes && 2 >= bytes.length) {
            return (short) ((bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF));
        } else {
            throw new BizException();
        }
    }
}
