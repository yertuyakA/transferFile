package kz.example.aibek;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"sftp.port=10222"})
public class ApplicationTest {
    @Autowired
    private SftpConfig.UploadGateway gateway;

    private static EmbedderSftpServer server;
    private static Path sftpFolder;

    @BeforeClass
    public static void startServer() throws Exception{
        server = new EmbedderSftpServer();
        server.setPort(10222);
        sftpFolder = Files.createTempDirectory("SFTP_UPLOAD_TEST");
        server.afterPropertiesSet();
        server.setHomeFolder(sftpFolder);
        if(!server.isRunning()){
            server.start();
        }
    }

    @Before
    @After
    public void cleanSftpFolder() throws IOException{
        Files.walk(sftpFolder).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void testUpload() throws IOException{
        Path tempFile = Files.createTempFile("UPLOAD_TEST", ".csv");
        assertEquals(0, Files.list(sftpFolder).count());
        gateway.upload(tempFile.toFile());

        List<Path> pathList = Files.list(sftpFolder).collect(Collectors.toList());
        assertEquals(1, pathList.size());
        assertEquals(tempFile.getFileName(), pathList.get(0).getFileName());
    }


    @AfterClass
    public static void stopServer(){
        if(server.isRunning()){
            server.stop();
        }
    }
}
