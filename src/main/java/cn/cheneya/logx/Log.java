package cn.cheneya.logx;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private enum Level {
        INFO, ERROR, WARN, DEBUG
    }

    public void info(String msg) {
        log(Level.INFO, msg, true);
    }

    public void error(String msg) {
        log(Level.ERROR, msg, true);
    }

    public void warn(String msg) {
        log(Level.WARN, msg, true);
    }

    public void warnNoNewline(String msg) {
        log(Level.WARN, msg, false);
    }

    public void debug(String msg) {
        log(Level.DEBUG, msg, true);
    }

    private void log(Level level, String msg, boolean newline) {

        String timestamp = "[" + LocalDateTime.now().format(FORMATTER) + "]";
        String logMessage = String.format("%s [%s] %s",
                timestamp, level, msg);

        if (newline) {
            OUT.println(logMessage);
        } else {
            OUT.print(logMessage);
        }
    }
}