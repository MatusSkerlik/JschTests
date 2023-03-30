package sk.skerlik;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class AbstractJschDockerTest {

    protected static final JSch         jSch     = new JSch();
    protected static final List<String> jSchLogs = Collections.synchronizedList(new ArrayList<>());

    public static int getPort() {
        return 22;
    }

    public static int getMessageCount(Pattern pattern) {
        int preAuthMessageCount = 0;
        for (String log : jSchLogs) {
            if (pattern.matcher(log).find()) {
                preAuthMessageCount++;
            }
        }
        return preAuthMessageCount;
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
