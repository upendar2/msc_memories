package com.msc.memories.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SystemLogService {

    private final List<String> logBuffer = Collections.synchronizedList(new ArrayList<>());

    public void addLog(String level, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formattedLog = String.format("[%s] %s: %s", timestamp, level, message);
        
        logBuffer.add(formattedLog);
        
        // Keep memory footprint small (keep last 200 logs)
        if (logBuffer.size() > 200) {
            logBuffer.remove(0);
        }
    }

    public List<String> getRecentLogs() {
        return new ArrayList<>(logBuffer);
    }
}