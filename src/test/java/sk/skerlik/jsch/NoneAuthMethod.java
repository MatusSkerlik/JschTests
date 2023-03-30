package sk.skerlik.jsch;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserAuthNone;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;

@Slf4j
public class NoneAuthMethod extends UserAuthNone {
    @Override
    public boolean start(Session session) throws Exception {
        try {
            return super.start(session);
        } catch (SocketTimeoutException e) {
            log.info("Cached socket timeout exception, returning false");
            session.setTimeout(30_000);
            return false;
        }
    }

}
