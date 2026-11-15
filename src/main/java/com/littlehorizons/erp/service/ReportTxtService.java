package com.littlehorizons.erp.service;

import com.littlehorizons.erp.util.AppLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes TXT reports under the reports/ directory.
 */
public class ReportTxtService {

    private static final Path REPORTS_DIR = Path.of("reports");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Path writeReport(String reportKey, String title, List<String> lines) {
        try {
            Files.createDirectories(REPORTS_DIR);
            String fileName = reportKey + "_" + LocalDateTime.now().format(FILE_TS) + ".txt";
            Path filePath = REPORTS_DIR.resolve(fileName);

            StringBuilder content = new StringBuilder();
            content.append("============================================================\n");
            content.append("Little Horizons ERP - Report\n");
            content.append(title).append('\n');
            content.append("Generated: ").append(LocalDateTime.now().format(DISPLAY_TS)).append('\n');
            content.append("============================================================\n\n");
            if (lines == null || lines.isEmpty()) {
                content.append("(no data)\n");
            } else {
                lines.forEach(line -> content.append(line).append('\n'));
            }
            content.append("\n------------------------------------------------------------\n");
            content.append("End of report\n");

            Files.writeString(filePath, content.toString(), StandardCharsets.UTF_8);
            return filePath.toAbsolutePath();
        } catch (IOException exception) {
            AppLogger.error("Failed to write report " + reportKey, exception);
            throw new IllegalStateException("Unable to write TXT report.", exception);
        }
    }
}
