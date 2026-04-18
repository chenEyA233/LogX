package cnm;

import cn.cheneya.logx.Log;
import cn.cheneya.logx.Version;

/**
 * Main entry point for the application.
 *
 * @author chenEyA
 */
public class Main {

    /**
     * Private constructor to prevent instantiation.
     * This class contains only static methods.
     */
    private Main() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the version of the application.
     *
     * @return the version string from Version class
     */
    public static String getVersion() {
        return Version.VERSION;
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Log logger = new Log();
        logger.info("Test text");
    }
}
