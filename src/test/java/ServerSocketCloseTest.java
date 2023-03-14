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

import java.lang.reflect.Field;
import java.net.Socket;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Testcontainers
public class ServerSocketCloseTest extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("main.py", "docker/server_socket_close_test/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/server_socket_close_test/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

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
    }

    @Test
    @DisplayName("Server will send identification string and closes socket. Jsch should throw exception and disconnect.")
    void test_0() throws NoSuchFieldException, IllegalAccessException {
        Session sessionSpy = spy(session);

        Assertions.assertThrows(JSchException.class, () -> sessionSpy.connect(), "JSch should throw exception, because socket is closed immediately after write of identification string");

        Field jsocket = Session.class.getDeclaredField("socket");
        jsocket.setAccessible(true);
        Socket socket = (Socket) jsocket.get(session);
        Assertions.assertNull(socket);

        // verify that disconnect was also called
        verify(sessionSpy).disconnect();
    }
}
