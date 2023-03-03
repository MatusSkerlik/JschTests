import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This class represents a JUnit test that tests the behavior of an SSH client when the server sends an SSH_MSG_CHANNEL_CLOSE message to close the shell channel.
 * <p>
 * The test uses the Testcontainers library to create and manage a Docker container running an SSH server for testing purposes. The client is implemented using the JSch library to establish an SSH session and open a shell channel with the server.
 * </p>
 * <p>
 * The {@link #test_0()} method performs the following steps:
 * </p>
 * <ol>
 * <li>Connects to the SSH server using the JSch library and opens a shell channel.</li>
 * <li>Waits for one second to allow the server to send its initial message.</li>
 * <li>Verifies that the channel is not connected and is closed, which indicates that the server has sent an `SSH_MSG_CHANNEL_CLOSE` message to close the channel.</li>
 * </ol>
 * <p>
 * If the test method fails, it means that the client has not properly handled the SSH_MSG_CHANNEL_CLOSE message and the channel remains open, or that the client has not properly disconnected from the server after detecting the closed channel.
 * </p>
 */
@Testcontainers
public class ChannelCloseTest extends AbstractJschDockerTest {

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "docker/common/server.py")
                    .withFileFromClasspath("main.py", "docker/channel_close_server/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/channel_close_server/Dockerfile")
    ).withExposedPorts(22);

    private static final String  USERNAME = "username";
    private static final String  PASSWORD = "password";
    private static       Session session;

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
    @DisplayName("Jsch session close after invalid message from server")
    void test_0() throws JSchException, InterruptedException {
        session.connect();
        ChannelShell shell = (ChannelShell) session.openChannel("shell");
        shell.connect();

        Thread.sleep(1000);

        Assertions.assertFalse(shell.isConnected());
        Assertions.assertTrue(shell.isClosed());
    }
}
