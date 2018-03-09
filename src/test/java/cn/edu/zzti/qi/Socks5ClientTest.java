package cn.edu.zzti.qi;

import cn.edu.zzti.qi.client.Socks5Client;
import cn.edu.zzti.qi.model.Constants;
import cn.edu.zzti.qi.util.CodeUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socks5ClientTest {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(Constants.TEST_CLIENT_PORT);
        System.out.println("Client listening port: " + server.getLocalPort());
        try {
            while (true) {
                executor.execute(new Socks5Client("localhost", Constants.TEST_SERVER_PORT, server.accept()));
            }
        } finally {
            CodeUtils.close(server);
        }
    }
}
