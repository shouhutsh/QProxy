package cn.edu.zzti.qi;

import cn.edu.zzti.qi.model.Constants;
import cn.edu.zzti.qi.server.Socks5Server;
import cn.edu.zzti.qi.util.CodeUtils;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socks5ServerTest {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception{
        ServerSocket server = new ServerSocket(Constants.TEST_SERVER_PORT);
        System.out.println("Server listening port: " + server.getLocalPort());
        try {
            while (true) {
                executor.execute(new Socks5Server(server.accept()));
            }
        } finally {
            CodeUtils.close(server);
        }
    }
}
