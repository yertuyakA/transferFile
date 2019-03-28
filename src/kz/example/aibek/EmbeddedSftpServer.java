package kz.example.aibek;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.SocketUtils;


public class EmbeddedSftpServer implements InitializingBean, SmartLifecycle {

    public static final int PORT = SocketUtils.findAvailableTcpPort();



    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void stop(Runnable runnable) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
