package cn.cheneya.logx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GetTime {
    public static String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm:ss.SSS");
        return LocalDateTime.now().format(formatter);
    }
}