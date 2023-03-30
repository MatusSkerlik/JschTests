package sk.skerlik;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Pattern;

@Slf4j
@Testcontainers
public class ClientChannelCloseTest extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "docker/server.py")
                    .withFileFromClasspath("main.py", "docker/client_channel_close_test/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/client_channel_close_test/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final Pattern LOG_PATTERN_0 = Pattern.compile("(?i).+exception.+leaving main loop");
    private static final Pattern LOG_PATTERN_1 = Pattern.compile("(?i).+disconnecting.+from.+port");

    private static Session session;

    @BeforeAll
    static void beforeAll() throws JSchException {
        session = jSch.getSession(USERNAME, sshd.getHost(), sshd.getFirstMappedPort());
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
    }

    @AfterAll
    static void afterAll() {
        session.disconnect();
    }

    @Test
    @DisplayName("Jsch channel close by server")
    void test_0() throws JSchException, InterruptedException {
        session.connect();
        ChannelShell shell = (ChannelShell) session.openChannel("shell");
        shell.connect();
        shell.disconnect();

        Thread.sleep(1000);

        boolean jschMessageTypeErrorFound0 = false;
        for (String log : jSchLogs) {
            if (LOG_PATTERN_0.matcher(log).find()) {
                jschMessageTypeErrorFound0 = true;
                break;
            }
        }

        boolean jschMessageTypeErrorFound1 = false;
        for (String log : jSchLogs) {
            if (LOG_PATTERN_1.matcher(log).find()) {
                jschMessageTypeErrorFound1 = true;
                break;
            }
        }

        Assertions.assertFalse(shell.isConnected());
        Assertions.assertTrue(shell.isClosed());
        Assertions.assertFalse(jschMessageTypeErrorFound0, "Main loop should not exit if all channels are closed in session");
        Assertions.assertFalse(jschMessageTypeErrorFound1, "Socket should not disconnect from socket if channel is closed");
    }
}
