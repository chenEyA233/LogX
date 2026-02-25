package cn.cheneya.logx;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final StackWalker STACK_WALKER;
    private SaveLog saveLog;

    public Log(){
        init();
    }

    private boolean isInit = false;

    private enum Level {
        INFO, ERROR, WARN, DEBUG
    }

    /**
     * 请不要多次调用这个方法bro
     */
    public void init(){
        isInit = true;
    }

    public void info(String msg) {
        if(isInit){log(Level.INFO, msg, true);}
    }

    public void error(String msg) {
        if(isInit){log(Level.ERROR, msg, true);}
    }

    public void warn(String msg) {
        if(isInit){log(Level.WARN, msg, true);}
    }

    public void warnNoNewline(String msg) {
        if(isInit){log(Level.WARN, msg, false);}
    }

    public void debug(String msg) {
        if(isInit){log(Level.DEBUG, msg, true);}
    }

    private void log(Level level, String msg, boolean newline) {

        String timestamp = "[" + LocalDateTime.now().format(FORMATTER) + "]";
        String className = getClassName();
        String logMessage = String.format("%s [%s/%s] %s",
                timestamp, className, level, msg);

        if (newline) {
            OUT.println(logMessage);
        } else {
            OUT.print(logMessage);
        }
    }

    public static String getClassName() {
        return STACK_WALKER.getCallerClass().getName();
    }

    static {
        STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    }
}