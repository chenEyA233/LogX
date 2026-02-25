package cn.cheneya.logx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaveLog {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final List<String> logBuffer = new ArrayList<>();
    private boolean isBuffering = false;

    public SaveLog(){
        Log logger = new Log();
        logger.info("Save logs...");
    }

    public void startBuffering() {
        isBuffering = true;
        logBuffer.clear();
    }

    public void stopBuffering() {
        isBuffering = false;
    }

    public void addLog(String className, String level, String message) {
        String timestamp = "[" + LocalDateTime.now().format(FORMATTER) + "]";
        String logEntry = String.format("%s [%s/%s] %s", timestamp, className, level, message);
        
        if (isBuffering) {
            logBuffer.add(logEntry);
        } else {
            try {
                Files.write(Paths.get("app.log"), (logEntry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Failed to write log: " + e.getMessage());
            }
        }
    }

    public void saveLog(File f) {
        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f, true), StandardCharsets.UTF_8))) {
                
                for (String logEntry : logBuffer) {
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

    public void saveCurrentLogs(File f) {
        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            File currentLogFile = new File("app.log");
            if (currentLogFile.exists()) {
                List<String> lines = Files.readAllLines(currentLogFile.toPath(), StandardCharsets.UTF_8);
                
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(f, true), StandardCharsets.UTF_8))) {
                    
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                    
                    writer.flush();
                }
                
                currentLogFile.delete();
            }
        } catch (IOException e) {
            System.err.println("Failed to save current logs: " + e.getMessage());
        }
    }

    public void clearLogs() {
        logBuffer.clear();
        try {
            Files.deleteIfExists(Paths.get("app.log"));
        } catch (IOException e) {
            System.err.println("Failed to clear logs: " + e.getMessage());
        }
    }
}
