package cnm;

import cn.cheneya.logx.Log;

/**
 * Debug test class for testing debug-level logging.
 *
 * @author chenEyA
 */
public class DebugTest {

    /**
     * Private constructor to prevent instantiation.
     * This class contains only static methods.
     */
    private DebugTest() {
        throw new UnsupportedOperationException();
    }

    /**
     * Main method to test debug logging functionality.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Log logger = new Log();
        logger.debug("Test debug text");
    }
}
