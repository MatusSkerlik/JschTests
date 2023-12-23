package sk.skerlik;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
@Slf4j
@Testcontainers
public class ServerChangePasswordTest extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "app/server/server.py")
                    .withFileFromClasspath("main.py", "app/server_change_password_test/main.py")
                    .withFileFromClasspath("Dockerfile", "app/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

    private static final String  USERNAME = "username";
    private static final String  PASSWORD = "password";
    private static       Session session;

    @BeforeAll
    static void beforeAll() throws JSchException {
        session = jSch.getSession(USERNAME, sshd.getHost(), sshd.getFirstMappedPort());
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");

        // Set keyboard interactive prompt handler
        session.setUserInfo(new UserInfoKeyboardInteractiveHandler());
    }

    @AfterAll
    static void afterAll() {
        session.disconnect();
    }

    @Test
    @DisplayName("Server will send prompt, which are asking to change password." +
            "Exception SHOULD be thrown from implemented UserAuth")
    void test_0() throws JSchException, InterruptedException {
        Session sessionSpy = spy(session);

        Assertions.assertThrows(IllegalStateException.class, sessionSpy::connect, "Server should prompt for old password, which should result in throw in keyboard-interactive user info handler.");
        Thread.sleep(1000);

        // verify that disconnect was also called
        verify(sessionSpy).disconnect();
    }

    static class UserInfoKeyboardInteractiveHandler implements UserInfo, UIKeyboardInteractive {

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompts, boolean[] echo) {
            for (String prompt: prompts){
                log.info("Received PROMPT from server: '{}'", prompt);
            }

            for (String prompt: prompts) {
                if (prompt.contains("old password") || prompt.contains("new password")) {
                    throw new IllegalStateException("Received CHANGE PASSWORD REQUEST from server.");
                }
            }

            return null;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassword(String message) {
            log.info("Recieved PROMPT PASSWORD from server: '{}'", message);
            return false;
        }

        @Override
        public boolean promptPassphrase(String message) {
            log.info("Recieved PROMPT PASSPHRASE from server: '{}'", message);
            return false;
        }

        @Override
        public boolean promptYesNo(String message) {
            log.info("Recieved PROMPT YES/NO from server: '{}'", message);
            return false;
        }

        @Override
        public void showMessage(String message) {
            log.info("Recieved MESSAGE from server: '{}'", message);
        }
    }
}
