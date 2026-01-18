package cnm;

import cn.cheneya.logx.Log;
import cn.cheneya.logx.Version;

public class Main {
    public static String getVersion() {
        return Version.VERSION;
    }

    public static void main(String[] args) {
        Log logger = new Log();
        logger.info("Test text");
    }
}