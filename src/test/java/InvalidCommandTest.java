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

import java.util.regex.Pattern;

/**
 * This class represents a client that connects to an SSH server and reacts to an invalid message type with a code
 * of 101 by disconnecting from the server.
 * <p>
 * The client uses the JSch library to establish an SSH session and open a shell channel with the server.
 * The Testcontainers library is used to create and manage a Docker container running the SSH server for testing purposes.
 * </p>
 * <p>
 * The {@link #test_0()} method is a JUnit test method that performs the following steps:
 * </p>
 * <ol>
 * <li>Connects to the SSH server using the JSch library and opens a shell channel.</li>
 * <li>Waits for one second to allow the server to send its initial message.</li>
 * <li>Verifies that the channel is not connected and is closed, which indicates that the server has sent an invalid message with a code of 101.</li>
 * <li>Searches the JSch logs for a message indicating that the library has disconnected from the server due to an unknown message type.</li>
 * <li>Asserts that such a message is found, indicating that the client has properly handled the invalid message and disconnected from the server.</li>
 * </ol>
 * <p>
 * If the test method fails, it means that the client has not properly handled the invalid message and the channel remains open, or that the client has not properly disconnected from the server after detecting the invalid message.
 * </p>
 */
@Testcontainers
public class InvalidCommandTest extends AbstractJschDockerTest {

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "docker/common/server.py")
                    .withFileFromClasspath("main.py", "docker/invalid_command_server/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/invalid_command_server/Dockerfile")
    ).withExposedPorts(22);
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final Pattern LOG_PATTERN = Pattern.compile("(?i).+exception.+leaving main loop.+unknown.+message.+type");
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
    @DisplayName("Jsch session close after invalid message from server")
    void test_0() throws JSchException, InterruptedException {
        session.connect();
        ChannelShell shell = (ChannelShell) session.openChannel("shell");
        shell.connect();

        Thread.sleep(1000);

        Assertions.assertFalse(shell.isConnected());
        Assertions.assertTrue(shell.isClosed());

        boolean jschMessageTypeErrorFound = false;
        for (String log : jSchLogs) {
            if (LOG_PATTERN.matcher(log).find()) {
                jschMessageTypeErrorFound = true;
                break;
            }
        }

        Assertions.assertTrue(jschMessageTypeErrorFound, "Jsch did not end with unknown message type as predicted");
    }
}
