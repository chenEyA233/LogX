package cn.cheneya.logx;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages log persistence to file system with support for buffering and batch writes.
 *
 * <p>This class provides functionality to:
 * <ul>
 *   <li>Buffer log entries in memory before writing to disk</li>
 *   <li>Write logs directly to a file in real-time</li>
 *   <li>Batch flush buffered logs to a specified file</li>
 *   <li>Clear and reset log files</li>
 *   <li>Filter logs by level (with or without DEBUG)</li>
 *   <li>Timestamped log file naming</li>
 * </ul>
 *
 * <p>This class implements a singleton pattern to ensure consistent
 * file access across the application.
 *
 * @author chenEyA
 */
@Getter
public class SaveLog {

    /**
     * Enum defining the log file naming mode.
     */
    public enum Mode {
        /**
         * Normal mode: uses the specified file name directly.
         */
        NORMAL,

        /**
         * Timestamp mode: appends timestamp to file name.
         * Format: filename-YYYY:MM:DD:HH:mm:ss.SSS.log
         */
        WITH_TIME
    }

    /**
     * Singleton instance using eager initialization for thread safety.
     */
    private static final SaveLog INSTANCE = new SaveLog();

    /**
     * DateTimeFormatter for generating consistent timestamp format (HH:mm:ss.SSS).
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * DateTimeFormatter for generating file timestamp format (yyyy.MM.dd-HH-mm-ss.SSS).
     */
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd-HH-mm-ss.SSS");

    /**
     * Default log file name for real-time logging.
     */
    private static final String DEFAULT_LOG_FILE = "log.log";

    /**
     * Thread-safe list for buffering log entries.
     * Using CopyOnWriteArrayList for safe concurrent access during reads/writes.
     */
    private final List<String> logBuffer = new CopyOnWriteArrayList<>();

    /**
     * Flag indicating whether buffering mode is active.
     * When true, logs are stored in memory instead of being written to disk.
     */
    @Setter
    private volatile boolean isBuffering = false;

    /**
     * Current save mode for log file naming.
     * Default is WITH_TIME mode.
     */
    private volatile Mode saveMode = Mode.WITH_TIME;

    /**
     * Base file name for timestamp mode.
     * Stores the original file name before timestamp is appended.
     */
    private String baseFileName = "log";

    /**
     * Current log file path.
     * Used in WITH_TIME mode to write all logs to the same file.
     */
    private String currentLogFilePath = DEFAULT_LOG_FILE;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SaveLog() {
        // Private constructor for singleton pattern
    }

    /**
     * Returns the singleton instance of SaveLog.
     *
     * @return the singleton SaveLog instance
     */
    public static SaveLog getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the log file save mode.
     *
     * <p>In NORMAL mode, logs are saved to the specified file name.
     * In WITH_TIME mode, the timestamp is appended to the file name:
     * filename-YYYY:MM:DD:HH:mm:ss.SSS.log
     *
     * @param mode the save mode to set
     */
    public void setSaveLogFileMode(Mode mode) {
        this.saveMode = mode;
    }

    /**
     * Starts buffering mode for log entries.
     * When buffering is enabled, logs are stored in memory instead of
     * being written to disk, allowing for batch operations.
     *
     * <p>This method also clears any existing buffered logs.
     */
    public synchronized void startBuffering() {
        isBuffering = true;
        logBuffer.clear();
    }

    /**
     * Stops buffering mode and returns to immediate write mode.
     * Any subsequent logs will be written directly to the default log file.
     */
    public synchronized void stopBuffering() {
        isBuffering = false;
    }

    /**
     * Gets an unmodifiable view of the buffered logs.
     * Useful for inspection without modification.
     *
     * @return an unmodifiable list of buffered log entries
     */
    public List<String> getBufferedLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logBuffer));
    }

    /**
     * Adds a log entry to the buffer or writes it directly to file,
     * depending on the current buffering state.
     *
     * <p>The log entry is formatted as: [timestamp] [ClassName/Level] message
     *
     * @param className the name of the class that generated the log
     * @param level     the log level (INFO, ERROR, WARN, DEBUG)
     * @param message   the log message content
     */
    public void addLog(String className, String level, String message) {
        // Generate formatted timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String logEntry = String.format("[%s] [%s/%s] %s", timestamp, className, level, message);

        if (isBuffering) {
            // Store in memory buffer
            logBuffer.add(logEntry);
        } else {
            // Write directly to file
            writeToFile(logEntry);
        }
    }

    /**
     * Writes a single log entry to the current log file.
     * Creates the file if it doesn't exist and appends to existing content.
     *
     * @param logEntry the formatted log entry to write
     */
    private void writeToFile(String logEntry) {
        try {
            // Ensure file path is initialized for WITH_TIME mode
            if (saveMode == Mode.WITH_TIME && currentLogFilePath.equals(DEFAULT_LOG_FILE)) {
                currentLogFilePath = resolveTargetFileName();
            }

            Path logPath = Paths.get(currentLogFilePath);
            String line = logEntry + System.lineSeparator();
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    /**
     * Resolves the target file name based on the current save mode.
     *
     * @return the resolved file name with timestamp if in WITH_TIME mode
     */
    private String resolveTargetFileName() {
        if (saveMode == Mode.WITH_TIME) {
            String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
            return baseFileName + "-" + timestamp + ".log";
        }
        return baseFileName + ".log";
    }

    /**
     * Saves all buffered logs to the specified file.
     * Only saves logs with level INFO, WARN, or ERROR (excludes DEBUG).
     *
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Creates parent directories if they don't exist</li>
     *   <li>Creates the file if it doesn't exist</li>
     *   <li>Appends filtered buffered logs (INFO/WARN/ERROR) to the file</li>
     *   <li>Clears the buffer after successful write</li>
     * </ol>
     *
     * @param file the destination file for the buffered logs
     */
    public void saveLog(File file) {
        saveLogWithFilter(file, true);
    }

    /**
     * Saves all buffered logs to the specified file.
     * Includes all log levels (INFO, WARN, ERROR, and DEBUG).
     *
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Creates parent directories if they don't exist</li>
     *   <li>Creates the file if it doesn't exist</li>
     *   <li>Appends all buffered logs to the file</li>
     *   <li>Clears the buffer after successful write</li>
     * </ol>
     *
     * @param file the destination file for the buffered logs
     */
    public void saveLogWithDebug(File file) {
        saveLogWithFilter(file, false);
    }

    /**
     * Internal method to save logs with optional DEBUG filtering.
     *
     * @param file      the destination file
     * @param skipDebug true to skip DEBUG logs, false to include all
     */
    private void saveLogWithFilter(File file, boolean skipDebug) {
        if (logBuffer.isEmpty()) {
            return;
        }

        try {
            File targetFile = resolveTargetFile(file);
            ensureFileExists(targetFile);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(targetFile, true), StandardCharsets.UTF_8))) {

                for (String logEntry : logBuffer) {
                    if (skipDebug && containsDebug(logEntry)) {
                        continue;
                    }
                    writer.write(logEntry);
                    writer.newLine();
                }

                writer.flush();
                logBuffer.clear();
            }
        } catch (IOException e) {
            System.err.println("Failed to save logs to file: " + e.getMessage());
        }
    }

    /**
     * Checks if a log entry contains DEBUG level.
     *
     * @param logEntry the log entry to check
     * @return true if the entry contains DEBUG, false otherwise
     */
    private boolean containsDebug(String logEntry) {
        return logEntry.contains("/DEBUG]");
    }

    /**
     * Resolves the target file based on the current save mode.
     *
     * @param file the original file
     * @return the resolved file with timestamp if in WITH_TIME mode
     */
    private File resolveTargetFile(File file) {
        if (saveMode == Mode.WITH_TIME) {
            String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                String name = fileName.substring(0, dotIndex);
                String ext = fileName.substring(dotIndex);
                return new File(file.getParentFile(), name + "-" + timestamp + ext);
            } else {
                return new File(file.getParentFile(), fileName + "-" + timestamp);
            }
        }
        return file;
    }

    /**
     * Saves a single line of log to the default log file.
     * This method writes directly to file without buffering.
     *
     * <p>The line is written with a timestamp prefix: [timestamp] message
     *
     * @param log the log message to write
     */
    public void saveLogWithOneLine(String log) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String logEntry = String.format("[%s] %s", timestamp, log);

        try {
            Path logPath = Paths.get(DEFAULT_LOG_FILE);
            String line = logEntry + System.lineSeparator();
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    /**
     * Clears/empties the specified log file.
     * If the file exists, its contents will be deleted.
     *
     * @param file the log file to clean
     */
    public void cleanLogFile(File file) {
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            System.err.println("Failed to clean log file: " + e.getMessage());
        }
    }

    /**
     * Saves the current contents of the default log file to a specified destination.
     *
     * <p>This method reads all lines from "log.log", writes them to the
     * destination file, and then deletes the source file.
     *
     * @param file the file to receive the current logs
     */
    public void saveCurrentLogs(File file) {
        try {
            File sourceFile = new File(DEFAULT_LOG_FILE);

            if (!sourceFile.exists()) {
                return;
            }

            File targetFile = resolveTargetFile(file);
            ensureFileExists(targetFile);

            // Read all lines from source file
            List<String> lines = Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);

            // Write to destination file
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(targetFile, true), StandardCharsets.UTF_8))) {

                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }

                writer.flush();
            }

            // Delete the source file after successful transfer
            Files.delete(sourceFile.toPath());

        } catch (IOException e) {
            System.err.println("Failed to save current logs: " + e.getMessage());
        }
    }

    /**
     * Ensures that the specified file exists, creating parent directories
     * and the file itself if necessary.
     *
     * @param file the file to ensure exists
     * @throws IOException if file creation fails
     */
    private void ensureFileExists(File file) throws IOException {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
    }

    /**
     * Clears both the log buffer and the default log file.
     * Use this method when starting a fresh logging session.
     */
    public void clearLogFile() {
        logBuffer.clear();
        try {
            Files.deleteIfExists(Paths.get(DEFAULT_LOG_FILE));
        } catch (IOException e) {
            System.err.println("Failed to clear logs: " + e.getMessage());
        }
    }

    /**
     * Returns the number of log entries currently in the buffer.
     *
     * @return the number of buffered log entries
     */
    public int getBufferSize() {
        return logBuffer.size();
    }
}
