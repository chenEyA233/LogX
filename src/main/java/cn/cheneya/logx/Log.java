package cn.cheneya.logx;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A lightweight logging utility that provides thread-safe logging capabilities
 * with support for console output and optional file persistence.
 *
 * <p>This class supports multiple log levels (INFO, ERROR, WARN, DEBUG)
 * with timestamp and caller class information.
 *
 * <p>Example usage:
 * <pre>{@code
 * Log logger = new Log();
 * logger.info("Application started");
 * logger.error("An error occurred");
 * logger.debug("Debug information");
 * }</pre>
 *
 * @author ChenEya
 */
public class Log {

    /**
     * Output stream for console logging, configured with UTF-8 encoding.
     * Using PrintStream allows for both println and print operations.
     */
    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    // ANSI color codes for terminal output
    private static final String ANSI_RESET = "\u001b[0m";
    private static final String ANSI_BLUE = "\u001b[34m";
    private static final String ANSI_GREEN = "\u001b[32m";
    private static final String ANSI_YELLOW = "\u001b[33m";
    private static final String ANSI_RED = "\u001b[31m";
    private static final String ANSI_WHITE = "\u001b[1;37m";

    /**
     * DateTimeFormatter for generating consistent timestamp format (HH:mm:ss.SSS).
     * This format provides millisecond precision for log entries.
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * StackWalker instance for efficiently retrieving caller class information.
     * Using StackWalker is more performant than SecurityManager for stack trace analysis.
     */
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /**
     * Singleton instance of Log using double-checked locking pattern.
     */
    private static volatile Log instance;

    /**
     * Reference to the SaveLog service for file persistence.
     * Initialized lazily when file saving is enabled.
     */
    private SaveLog saveLog;

    /**
     * Flag indicating whether the logger has been initialized.
     * Prevents multiple initializations and ensures clean state.
     */
    private volatile boolean isInitialized = false;

    /**
     * Flag indicating whether logs should be persisted to file.
     * When true, all log entries are also written to the log file.
     */
    private volatile boolean saveToFile = false;

    /**
     * Flag to enable clickable links in IDE console.
     * When true, log output uses format: ClassName(File.java:line) which is
     * recognized by IntelliJ IDEA for click-to-navigate functionality.
     */
    private volatile boolean clickableLinkEnabled = false;

    /**
     * Flag to control whether to print save log notice when SaveLog is instantiated.
     * Public for backward compatibility.
     */
    public boolean saveLogNotice = false;

    /**
     * Enum representing the available log severity levels.
     * Levels are ordered by severity: DEBUG &lt; INFO &lt; WARN &lt; ERROR
     */
    public enum Level {
        /** Debug level for detailed diagnostic information */
        DEBUG,
        /** Info level for general application events */
        INFO,
        /** Warning level for potentially harmful situations */
        WARN,
        /** Error level for serious errors that prevent certain functionality */
        ERROR
    }

    /**
     * Constructs a new Log instance and initializes the logger.
     * Automatically clears existing logs and prepares for new logging session.
     *
     * <p>This constructor maintains backward compatibility with existing code.
     */
    public Log() {
        init();
    }

    /**
     * Constructs a new Log instance without automatic initialization.
     *
     * @param init if true, the logger will be initialized; if false, it will remain uninitialized
     * @deprecated Use {@link #Log()} for automatic initialization, or call {@link #init()} manually
     */
    @Deprecated
    public Log(boolean init) {
        if (init) {
            init();
        }
    }

    /**
     * Returns the singleton instance of the Log class.
     * Uses double-checked locking to ensure thread-safe lazy initialization.
     *
     * @return the singleton Log instance
     */
    public static Log getInstance() {
        if (instance == null) {
            synchronized (Log.class) {
                if (instance == null) {
                    instance = new Log();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the logger and clears any existing log files.
     * Automatically enables file saving with default settings.
     *
     * <p>Note: Calling this method multiple times is safe but will clear
     * the log file each time.
     */
    public synchronized void init() {
        if (!isInitialized) {
            this.saveLog = SaveLog.getInstance();
            SaveLog.getInstance().clearLogFile();
            // Enable automatic file saving with default settings
            this.saveToFile = true;
            isInitialized = true;
        }
    }

    /**
     * Enables file logging functionality.
     * When enabled, all log entries will be written to a file in addition
     * to console output.
     */
    public void enableFileSaving() {
        this.saveToFile = true;
    }

    /**
     * Enables clickable links in IDE console output.
     * When enabled, log output uses the format recognized by IntelliJ IDEA:
     * ClassName(File.java:line)
     *
     * <p>This allows clicking on the class name in the console to navigate
     * directly to the source code location.
     *
     * <p>Note: Requires IDEA setting "处理终端输出以查找并高亮显示类名" to be enabled
     * (Settings | Advanced Settings | JVM Languages | Process console output to find
     * and highlight class names).
     */
    public void enableClickableLinks() {
        this.clickableLinkEnabled = true;
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param msg the message to be logged
     */
    public void info(String msg) {
        if (isInitialized) {
            log(Level.INFO, msg, true);
        }
    }

    /**
     * Logs a message at the ERROR level.
     * Typically used for errors that prevent specific operations.
     *
     * @param msg the message to be logged
     */
    public void error(String msg) {
        if (isInitialized) {
            log(Level.ERROR, msg, true);
        }
    }

    /**
     * Logs a message at the WARN level.
     * Used for potentially harmful situations that warrant attention.
     *
     * @param msg the message to be logged
     */
    public void warn(String msg) {
        if (isInitialized) {
            log(Level.WARN, msg, true);
        }
    }

    /**
     * Logs a message at the WARN level without a trailing newline.
     * Useful for progress indicators or status updates.
     *
     * @param msg the message to be logged
     */
    public void warnNoNewline(String msg) {
        if (isInitialized) {
            log(Level.WARN, msg, false);
        }
    }

    /**
     * Logs a message at the DEBUG level.
     * Used for detailed diagnostic information during development.
     *
     * @param msg the message to be logged
     */
    public void debug(String msg) {
        if (isInitialized) {
            log(Level.DEBUG, msg, true);
        }
    }

    /**
     * Logs the library version and author information.
     *
     * <p>This method outputs a formatted message containing:
     * <ul>
     *   <li>Library name and version</li>
     *   <li>Author information</li>
     *   <li>GitHub repository link</li>
     *   <li>Bilibili profile link</li>
     * </ul>
     *
     * <p>Example output:
     * <pre>[12:00:00.000] [cn.example.Main/INFO] LogX v2026.4.0 - Author: chenEyA - GitHub: <a href="https://github.com/chenEyA233">https://github.com/chenEyA233</a> - Bilibili: <a href="https://space.bilibili.com/3546694612420731">https://space.bilibili.com/3546694612420731</a></pre>
     *
     * @param level the log level at which to display the information
     */
    public void logxInformation(Level level) {
        if (isInitialized) {
            String message = String.format(
                    "LogX v%s - Author: chenEyA - GitHub: https://github.com/chenEyA233 - Bilibili: https://space.bilibili.com/3546694612420731",
                    Version.getVersion());
            log(level, message, true);
        }
    }

    /**
     * Core logging method that formats and outputs the log message.
     *
     * <p>Output formats:
     * <ul>
     *   <li>Simple: [timestamp] [ClassName/Level] message</li>
     *   <li>Clickable: [timestamp] ClassName(File.java:line) - Level - message</li>
     * </ul>
     *
     * @param level     the severity level of the log entry
     * @param msg       the message content to be logged
     * @param newline   whether to append a newline after the message
     */
    private void log(Level level, String msg, boolean newline) {
        // Generate timestamp in HH:mm:ss.SSS format
        String timestamp = "[" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "]";

        // Get the caller frame with class name, file name, and line number
        CallerInfo callerInfo = getCallerInfo();

        // Format the complete log message with colors
        String formattedMessage;
        if (clickableLinkEnabled) {
            // Format for IDEA clickable links: [ClassName(File.java:1)/Level] message
            String location = String.format("[%s(%s:1)/%s]", callerInfo.className, callerInfo.fileName, level);
            formattedMessage = String.format("%s%s%s %s%s%s %s%s%s",
                    ANSI_BLUE, timestamp, ANSI_RESET,
                    ANSI_GREEN, location, ANSI_RESET,
                    getLevelColor(level), msg, ANSI_RESET);
        } else {
            // Simple format: [ClassName/Level] message
            String location = String.format("[%s/%s]", callerInfo.className, level);
            formattedMessage = String.format("%s%s%s %s%s%s %s%s%s",
                    ANSI_BLUE, timestamp, ANSI_RESET,
                    ANSI_GREEN, location, ANSI_RESET,
                    getLevelColor(level), msg, ANSI_RESET);
        }

        // Output to console
        if (newline) {
            OUT.println(formattedMessage);
        } else {
            OUT.print(formattedMessage);
        }

        // Persist to file if enabled (use class name only, no link)
        if (saveToFile) {
            if (saveLog == null) {
                saveLog = SaveLog.getInstance();
            }
            saveLog.addLog(callerInfo.className, level.name(), msg);
        }
    }

    /**
     * Holds caller information for log formatting.
     */
    private static class CallerInfo {
        final String className;
        final String fileName;
        final int lineNumber;

        CallerInfo(String className, String fileName, int lineNumber) {
            this.className = className;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * Retrieves caller information including class name, file name, and line number
     * using StackWalker.
     *
     * <p>The stack frame skip count (3) accounts for:
     * <ol>
     *   <li>This method (log)</li>
     *   <li>Caller of log method</li>
     *   <li>StackWalker frames</li>
     * </ol>
     *
     * @return CallerInfo containing the caller's class name, file name, and line number
     */
    private CallerInfo getCallerInfo() {
        return STACK_WALKER.walk(frames -> frames
                .skip(3)
                .findFirst()
                .map(frame -> new CallerInfo(
                        frame.getClassName(),
                        frame.getFileName(),
                        frame.getLineNumber()
                ))
                .orElse(new CallerInfo("Unknown", "Unknown.java", 0)));
    }

    /**
     * Returns the ANSI color code for the given log level.
     *
     * @param level the log level
     * @return the ANSI color code string
     */
    private String getLevelColor(Level level) {
        return switch (level) {
            case INFO, DEBUG -> ANSI_WHITE;
            case WARN -> ANSI_YELLOW;
            case ERROR -> ANSI_RED;
        };
    }
}
