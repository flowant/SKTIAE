package org.flowant.stats;

import static org.flowant.stats.Config.SEP_DIR;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

public class BaseTest {
    static {
        try {
            Initializer.Initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String uniqueId;
    protected String testSourcePath = "src" + SEP_DIR + "test" + SEP_DIR + "java";
    protected Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    protected static Level l = Level.FINE;

    @Before
    public void setUp() {
        uniqueId = this.toString();
        logger.log(l, () -> "Test uniqueId:" + uniqueId);
    }

    @After
    public void tearDown() throws IOException {
    }
}
