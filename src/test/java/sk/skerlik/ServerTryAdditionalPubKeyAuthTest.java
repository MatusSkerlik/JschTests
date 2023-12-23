package sk.skerlik;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static org.mockito.Mockito.spy;

/**
 * This test class verifies the behavior of an SSH server that allows public key authentication based on a simple condition.
 * The server allows public key authentication for a user if its internal counter (i) is divisible by 2 or 3.
 */
@Testcontainers
public class ServerTryAdditionalPubKeyAuthTest extends AbstractJschDockerTest {

    private static final int PORT = getPort();

    @Container
    public static final GenericContainer<?> sshd = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromClasspath("server.py", "app/server/server.py")
                    .withFileFromClasspath("main.py", "app/server_public_auth_test/main.py")
                    .withFileFromClasspath("key.rsa", "app/server_public_auth_test/key.rsa")
                    .withFileFromClasspath("Dockerfile", "app/server_public_auth_test/Dockerfile")
    ).withExposedPorts(PORT).withEnv("PORT", Integer.toString(PORT));

    private static final Pattern PRE_AUTH_PATTERN = Pattern.compile("rsa-sha2-(?:256|512) preauth success");
    private static final Pattern AUTH_PATTERN     = Pattern.compile("rsa-sha2-(?:256|512) auth (?:success|failure)");
    private static final Pattern AUTH_FAILURE     = Pattern.compile("rsa-sha2-(?:256|512) auth failure");
    private static final String  USERNAME         = "username";
    private static final String  PRIVATE_KEY;
    private static       Session session;

    static {
        ClassLoader classLoader = ServerTryAdditionalPubKeyAuthTest.class.getClassLoader();
        File file = new File(classLoader.getResource("app/server_public_auth_test/key.rsa").getFile());
        try {
            PRIVATE_KEY = FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void beforeAll() throws JSchException {
        session = jSch.getSession(USERNAME, sshd.getHost(), sshd.getFirstMappedPort());
        jSch.addIdentity("", PRIVATE_KEY.getBytes(), null, null);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey");

        session.setConfig("enable_pubkey_auth_query", "yes");
        session.setConfig("try_additional_pubkey_algorithms", "yes");
    }

    @AfterAll
    static void afterAll() {
        session.disconnect();
    }


    @Test
    @DisplayName("Test try_additional_pubkey_algorithms option for public-key auth.")
    void test_0() throws JSchException {
        //     Attempt to authenticate using other signature algorithms supported by the same public key.
        //
        //    Some servers incorrectly respond with SSH_MSG_USERAUTH_PK_OK to the
        //    intial auth query, but then fail the full SSH_MSG_USERAUTH_REQUEST for
        //    RSA keys (which can support multiple signautre algorithms).
        //
        //    Allow the new behavior to potentially try other algorithms to be
        //    disabled via a new config option `try_additional_pubkey_algorithms`.
        //
        //    Additionally, add a new config option `use_pubkey_auth_query` to allow
        //    skipping auth queries and skip straight to attempting full
        //    SSH_MSG_USERAUTH_REQUEST's.

        Session sessionSpy = spy(session);
        sessionSpy.connect();

        //[main] INFO sk.skerlik.AbstractJschDockerTest - Next authentication method: publickey
        //[main] INFO sk.skerlik.AbstractJschDockerTest - PubkeyAcceptedAlgorithms = ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256
        //[main] INFO sk.skerlik.AbstractJschDockerTest - PubkeyAcceptedAlgorithms in server-sig-algs = [ssh-ed25519, ecdsa-sha2-nistp256, ecdsa-sha2-nistp384, ecdsa-sha2-nistp521, rsa-sha2-512, rsa-sha2-256]
        //[main] INFO sk.skerlik.AbstractJschDockerTest - rsa-sha2-512 preauth success
        //[main] INFO sk.skerlik.AbstractJschDockerTest - rsa-sha2-512 auth failure
        //[main] INFO sk.skerlik.AbstractJschDockerTest - rsa-sha2-256 preauth success
        //[main] INFO sk.skerlik.AbstractJschDockerTest - rsa-sha2-256 auth success
        //[main] INFO sk.skerlik.AbstractJschDockerTest - Authentication succeeded (publickey).

        int preAuthMessageCount = getMessageCount(PRE_AUTH_PATTERN);
        Assertions.assertEquals(2, preAuthMessageCount, "There should be two pre-auth messages");

        int authMessageCount = getMessageCount(AUTH_PATTERN);
        Assertions.assertEquals(2, authMessageCount, "There should be two auth messages");

        int authFailMessageCount = getMessageCount(AUTH_FAILURE);
        Assertions.assertEquals(1, authFailMessageCount, "There should be only one failed auth message");
    }
}
