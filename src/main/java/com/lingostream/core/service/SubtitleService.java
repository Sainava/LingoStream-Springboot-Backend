package com.lingostream.core.service;

import com.lingostream.core.entity.SubtitleEntity;
import com.lingostream.core.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;

    @Transactional
    public void parseAndSaveSrt(Resource resource, String videoId, String languageCode) {
        log.info("Starting ingestion for video: {} [{}]", videoId, languageCode);
        List<SubtitleEntity> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            Long startTimeMs = null;
            Long endTimeMs = null;
            StringBuilder contentBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Blank line means the end of the current subtitle block
                if (line.isEmpty()) {
                    if (startTimeMs != null && !contentBuilder.isEmpty()) {
                        batch.add(SubtitleEntity.builder()
                                .videoId(videoId)
                                .languageCode(languageCode)
                                .startTimeMs(startTimeMs)
                                .endTimeMs(endTimeMs)
                                .content(contentBuilder.toString())
                                .build());
                    }
                    // Reset state for the next block
                    startTimeMs = null;
                    endTimeMs = null;
                    contentBuilder = new StringBuilder();
                    continue;
                }

                // Detect the timestamp line
                if (line.contains("-->")) {
                    String[] times = line.split(" --> ");
                    startTimeMs = convertToMilliseconds(times[0].trim());
                    endTimeMs = convertToMilliseconds(times[1].trim());
                }
                // If it's not a timestamp and we already have a start time, it must be dialogue text
                else if (startTimeMs != null) {
                    if (!contentBuilder.isEmpty()) {
                        contentBuilder.append("\n"); // Handle multi-line dialogue securely
                    }
                    contentBuilder.append(line);
                }
            }

            // Save the entire batch to PostgreSQL at once for performance
            subtitleRepository.saveAll(batch);
            log.info("Successfully ingested {} subtitle rows.", batch.size());

        } catch (Exception e) {
            log.error("Failed to parse SRT file", e);
            throw new RuntimeException("SRT Parsing failed", e);
        }
    }

    // Helper method to convert "00:01:24,125" into purely milliseconds
    private Long convertToMilliseconds(String timeString) {
        String[] parts = timeString.split("[:,]");
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        long seconds = Long.parseLong(parts[2]);
        long millis = Long.parseLong(parts[3]);
        return (hours * 3600_000) + (minutes * 60_000) + (seconds * 1000) + millis;
    }
}