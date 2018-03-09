package cn.edu.zzti.qi.util;

import cn.edu.zzti.qi.exception.BizException;

import java.util.Random;

public final class CryptoUtils {

    private static final int BYTE_SIZE = 0x100;

    public static final byte[] createEncryptBytes(long seed) {
        boolean[] seats = new boolean[BYTE_SIZE];
        byte[] encryptMap = new byte[BYTE_SIZE];

        Random random = new Random(seed);
        for (int i = 0; i < BYTE_SIZE; ++i) {
            int idx = findAndSet(seats, i, random.nextInt(BYTE_SIZE));
            encryptMap[i] = (byte) (idx & 0xFF);
        }
        return encryptMap;
    }

    public static final byte[] createDecryptBytes(long seed) {
        byte[] decryptMap = new byte[BYTE_SIZE];
        byte[] encryptMap = createEncryptBytes(seed);
        for (int i = 0; i < encryptMap.length; ++i) {
            decryptMap[encryptMap[i] & 0xFF] = (byte) (i & 0xFF);
        }
        return decryptMap;
    }

    private static int findAndSet(boolean[] seats, int curIdx, int random) {
        for (int i = random; i < random + BYTE_SIZE; ++i) {
            int result = i % BYTE_SIZE;
            if (!seats[result] && result != curIdx){
                seats[result] = true;
                return result;
            }
        }
        throw new BizException("算法有误");
    }
}
