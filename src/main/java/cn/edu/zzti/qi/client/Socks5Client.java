package cn.edu.zzti.qi.client;

import cn.edu.zzti.qi.exception.BizException;
import cn.edu.zzti.qi.model.Constants;
import cn.edu.zzti.qi.model.HttpRequest;
import cn.edu.zzti.qi.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socks5Client implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String serverHost;
    private int serverPort;
    private Socket client;

    private String name = Constants.TEST_USER_NAME;
    private String passwd = Constants.TEST_USER_PASSWD;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public Socks5Client(String serverHost, int serverPort, Socket client) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.client = client;
    }

    @Override
    public void run() {
        Socket server = null;
        InputStream in = null;
        OutputStream out = null;
        InputStream newIn = null;
        OutputStream newOut = null;
        try {
            server = new Socket(serverHost, serverPort);
            in = server.getInputStream();
            out = server.getOutputStream();
            SocketUtils.write(out, new byte[]{0x05, 0x01, 0x02});
            in.read();
            if (0x02 == in.read()) {
                SocketUtils.write(out,
                        new byte[] {0x05},
                        new byte[] {(byte) name.length()},
                        name.getBytes(),
                        new byte[] {(byte) passwd.length()},
                        passwd.getBytes());
                in.read();
                if (0x00 == in.read()) {
                    newIn = client.getInputStream();
                    newOut = client.getOutputStream();

                    byte[] rawReq = SocketUtils.read(newIn);
                    HttpRequest httpRequest = HttpUtils.parseRequest(rawReq);
                    logger.info("浏览器发送数据：" + new String(rawReq));
                    String httpPort;
                    if (StrUtils.equalsIgnoreCase(Constants.HTTP_METHOD_CONNECT, httpRequest.getMethod())) {
                        httpPort = httpRequest.getPath();
                    } else {
                        httpPort = httpRequest.getHeaders().get(Constants.HTTP_HEADER_HOST);
                    }
                    if (StrUtils.isEmpty(httpPort)) {
                        throw new BizException();
                    }
                    String hostPort = httpPort;
                    String host = HttpUtils.getHost(hostPort);
                    int port = HttpUtils.getPort(hostPort);
                    SocketUtils.write(out,
                        new byte[]{0x05, 0x01, 0x00, 0x03},
                        new byte[]{(byte) host.getBytes().length},
                        host.getBytes(),
                        ByteUtils.transferShort((short) port));

                    byte[] bytes = new byte[7 + host.getBytes().length];
                    in.read(bytes);
                    if (0x00 == bytes[1]) {
                        CountDownLatch latch = new CountDownLatch(2);
                        InputStream curIn = new ByteArrayInputStream(rawReq);
                        if (StrUtils.equalsIgnoreCase(Constants.HTTP_METHOD_CONNECT, httpRequest.getMethod())) {
                            SocketUtils.write(newOut, "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                            curIn = newIn;
                        }
                        transfer(latch, curIn, out, CryptoUtils.createEncryptBytes(Constants.TEST_RANDOM_SEED));
                        transfer(latch, in, newOut, CryptoUtils.createDecryptBytes(Constants.TEST_RANDOM_SEED));
                        latch.await();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CodeUtils.close(in);
            CodeUtils.close(out);
            CodeUtils.close(newIn);
            CodeUtils.close(newOut);
            CodeUtils.close(client);
            CodeUtils.close(server);
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
