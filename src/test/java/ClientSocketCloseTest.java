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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
public class ClientSocketCloseTest extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "docker/common/server.py")
                    .withFileFromClasspath("main.py", "docker/client_socket_close_test/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/client_socket_close_test/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

    private static final String  USERNAME    = "username";
    private static final String  PASSWORD    = "password";
    private static final Pattern LOG_PATTERN = Pattern.compile("(?i).+exception.+leaving main loop.+socket.+closed");
    private static       Session session;

    @BeforeAll
    static void beforeAll() throws JSchException {
        session = jSch.getSession(USERNAME, sshd.getHost(), sshd.getFirstMappedPort());
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
    }

    @AfterAll
    static void afterAll() {
    }

    @Test
    @DisplayName("JSch will close session immediately after its creation, Socket closed exception simulation")
    void test_0() throws JSchException, InterruptedException {
        Session sessionSpy = spy(session);

        sessionSpy.connect();
        sessionSpy.disconnect();

        Thread.sleep(1000);

        boolean jschKexErrorFound = false;
        for (String log : jSchLogs) {
            if (LOG_PATTERN.matcher(log).find()) {
                jschKexErrorFound = true;
                break;
            }
        }

        Assertions.assertTrue(jschKexErrorFound, "JSch should close socket prematurely and we should get Socket closed exception");

        verify(sessionSpy, times(2)).disconnect();
    }
}
