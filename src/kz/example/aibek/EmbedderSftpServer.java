package kz.example.aibek;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Collections;

public class EmbedderSftpServer implements InitializingBean, SmartLifecycle {

    public static final int PORT = 0;
    private final SshServer server = SshServer.setUpDefaultServer();
    private volatile int port;
    private volatile boolean running;
    private DefaultSftpSessionFactory defaultSftpSessionFactory;


    public void setPort(int port) {
        this.port = port;
    }

    public void setDefaultSftpSessionFactory(DefaultSftpSessionFactory defaultSftpSessionFactory) {
        this.defaultSftpSessionFactory = defaultSftpSessionFactory;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        final PublicKey allowedKey = decodePublicKey();
        this.server.setPublickeyAuthenticator((username,key,session)->key.equals(allowedKey));
        this.server.setPort(this.port);
        this.server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Files.createTempFile("host_file","ser")));
        this.server.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(new SftpSubsystemFactory()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(Files.createTempDirectory("SFTP_TEMP")));
        server.setCommandFactory(new ScpCommandFactory());
    }

    public void setHomeFolder(Path path){
        server.setFileSystemFactory(new VirtualFileSystemFactory(path));
    }

    private PublicKey decodePublicKey() throws Exception {
        InputStream stream = new ClassPathResource("/keys/sftp_rsa.pub").getInputStream();
        byte[] decodeBuffer = Base64.decodeBase64(StreamUtils.copyToByteArray(stream));
//        byte[] keyBytes = StreamUtils.copyToByteArray(stream);
        // strip any newline chars
//        while (keyBytes[keyBytes.length - 1] == 0x0a || keyBytes[keyBytes.length - 1] == 0x0d) {
//            keyBytes = Arrays.copyOf(keyBytes, keyBytes.length - 1);
//        }
//        byte[] decodeBuffer = Base64Utils.decode(keyBytes);
//        byte[] decodeBuffer = Base64.decodeBase64()

        ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
        int len = bb.getInt();

        byte[] type = new byte[len];
        bb.get(type);
        if ("ssh-rsa".equals(new String(type))) {
            BigInteger e = decodeBigInt(bb);
            BigInteger m = decodeBigInt(bb);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } else {
            throw new IllegalArgumentException("Only supports RSA");
        }
    }

    private BigInteger decodeBigInt(ByteBuffer bb) {
        int len = bb.getInt();
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new BigInteger(bytes);
    }

    @Override
    public boolean isAutoStartup() {
        return PORT == this.port;
    }

    @Override
    public void stop(Runnable runnable) {
        stop();
        runnable.run();
    }

    @Override
    public void start() {
        try {
            server.start();
            this.running = true;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (this.running) {
            try {
                server.stop(false);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                this.running = false;
            }
        }
    }



    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
