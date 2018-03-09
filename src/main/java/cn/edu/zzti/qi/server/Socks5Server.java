package cn.edu.zzti.qi.server;

import cn.edu.zzti.qi.model.Constants;
import cn.edu.zzti.qi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socks5Server implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Socket client;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public Socks5Server(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        InputStream in = null;
        OutputStream out = null;
        try {
            logger.info("新的Socks5服务器线程");
            in = client.getInputStream();
            out = client.getOutputStream();

            int socksVersion = in.read();
            if (0x05 == socksVersion) {
                logger.info("判断为Socks5版本");
                int nmethods = in.read();
                byte[] methods = new byte[nmethods];
                in.read(methods);
                byte type = 0;
                for (byte m : methods) {
                    if(0x02 == m) {
                        type = m;
                    }
                }
                if (0x00 == type) {
                    logger.info("不验证处理");
                    SocketUtils.write(out, new byte[]{0x05, 0x00});
                    doSomething(in, out);
                } else if (0x02 == type) {
                    logger.info("验证用户名密码处理");
                    SocketUtils.write(out, new byte[]{0x05, 0x02});
                    in.read();
                    int nchar = in.read();
                    byte[] name = new byte[nchar];
                    in.read(name);
                    nchar = in.read();
                    byte[] passwd = new byte[nchar];
                    in.read(passwd);
                    if (StrUtils.equals(Constants.TEST_USER_NAME, new String(name))
                            && StrUtils.equals(Constants.TEST_USER_PASSWD, new String(passwd))) {
                        logger.info("用户名密码通过");
                        SocketUtils.write(out, new byte[]{0x05, 0x00});
                        doSomething(in, out);
                    } else {
                        logger.info("用户名密码错误");
                        SocketUtils.write(out, new byte[]{0x05, 0x01});
                    }
                } else {
                    SocketUtils.write(out, new byte[]{0x05, (byte) 0xFF});
                }
            } else {
                logger.error("不支持的socks版本");
            }
        } catch (Exception e) {
            logger.error("异常", e);
        } finally {
            CodeUtils.close(in);
            CodeUtils.close(out);
            CodeUtils.close(client);
            logger.info("Socks5服务器线程结束");
        }
    }

    private void doSomething(InputStream in, OutputStream out) throws IOException {
        logger.info("准备接收Socks5代理目标地址");
        int nchar;
        byte[] head = new byte[4];
        in.read(head);
        byte cmdType = head[1];
        byte addrType = head[3];
        byte[] hostRaw;
        String host;
        if (0x01 == addrType) {
            byte[] ipv4 = new byte[4];
            in.read(ipv4);
            hostRaw = ipv4;
            host = Inet4Address.getByAddress(ipv4).getHostAddress();
        } else if (0x03 == addrType) {
            nchar = in.read();
            byte[] domain = new byte[nchar];
            in.read(domain);
            hostRaw = new byte[1 + domain.length];
            hostRaw[0] = (byte) nchar;
            for (int i = 0; i < domain.length; ++i) {
                hostRaw[i + 1] = domain[i];
            }
            host = new String(domain);
        } else if (0x04 == addrType) {
            byte[] ipv6 = new byte[16];
            in.read(ipv6);
            hostRaw = ipv6;
            host = Inet6Address.getByAddress(ipv6).getHostAddress();
        } else {
            logger.info("接收Socks5地址类型不支持");
            SocketUtils.write(out,
                    new byte[]{0x05, 0x05, 0x00, 0x01},
                    new byte[6]);
            return;
        }

        byte[] port = new byte[2];
        in.read(port);
        int iport = ByteUtils.transferShort(port);

        logger.info(String.format("目标地址为：%s:%d", host, iport));
        if (0x01 == cmdType) {
            logger.info("要求进行Socks5 TCP代理");
            Socket socket = null;
            InputStream newIn = null;
            OutputStream newOut = null;
            try {
                socket = new Socket(host, iport);
                newIn = socket.getInputStream();
                newOut = socket.getOutputStream();
                SocketUtils.write(out,
                        new byte[]{0x05, 0x00, 0x00, addrType},
                        hostRaw,
                        port);
                logger.info("准备进行TCP代理");
                CountDownLatch latch = new CountDownLatch(2);
                transfer(latch, in, newOut, CryptoUtils.createDecryptBytes(Constants.TEST_RANDOM_SEED));
                transfer(latch, newIn, out, CryptoUtils.createEncryptBytes(Constants.TEST_RANDOM_SEED));

                latch.await();
                logger.info("交互完成");
            } catch (Exception e) {
                logger.error("异常", e);
            } finally {
                CodeUtils.close(newIn);
                CodeUtils.close(newOut);
                CodeUtils.close(socket);
            }
        } else if (0x02 == cmdType) {
            // TODO
        } else if (0x03 == cmdType) {
            // TODO
        } else {
            out.write(new byte[]{0x05, 0x07, 0x00, addrType});
            out.write(hostRaw);
            out.write(port);
            out.flush();
        }
    }

    private void transfer(final CountDownLatch latch, final InputStream in, final OutputStream out, byte[] byteMap) {
        executor.submit(() -> {
            try {
                SocketUtils.transfer(in, out, byteMap);
            } catch (Exception e) {
                logger.error("数据流转换失败", e);
            } finally {
                latch.countDown();
            }
        });
    }
}
