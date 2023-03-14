import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AbstractJschDockerTest {

    protected static final JSch         jSch     = new JSch();
    protected static final List<String> jSchLogs = Collections.synchronizedList(new ArrayList<>());

    public static int getPort() {
        return 22;
    }

    @BeforeAll
    static void _beforeAll() {
        jSch.setInstanceLogger(new Logger() {
            @Override
            public boolean isEnabled(int i) {
                return true;
            }

            @Override
            public void log(int i, String s) {
                log.info(s);
                jSchLogs.add(s);
            }
        });
    }
}
