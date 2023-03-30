package sk.skerlik;

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

@Testcontainers
public class ServerNoneAuthTimeoutException extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "docker/server.py")
                    .withFileFromClasspath("main.py", "docker/server_auth_none_socket_timeout/main.py")
                    .withFileFromClasspath("Dockerfile", "docker/server_auth_none_socket_timeout/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

    private static final String  USERNAME                          = "username";
    private static final String  PASSWORD                          = "password";
    private static final Pattern CUSTOM_NONE_CLASS_PACKAGE_PATTERN = Pattern.compile("(?im)sk\\.skerlik\\.jsch\\.NoneAuthMethod");
    private static       Session session;

    @BeforeAll
    static void beforeAll() throws JSchException {
        session = jSch.getSession(USERNAME, sshd.getHost(), sshd.getFirstMappedPort());
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("userauth.none", "sk.skerlik.jsch.NoneAuthMethod");
    }

    @AfterAll
    static void afterAll() {
        session.disconnect();
    }

    @Test
    @DisplayName("Server will timeout on user auth none, jsch will continue in next auth methods")
    void test_0() throws JSchException {
        session.connect(2000);
        ChannelShell shell = (ChannelShell) session.openChannel("shell");
        shell.connect();

        Assertions.assertTrue(session.isConnected());
        Assertions.assertTrue(shell.isConnected());
    }
}
