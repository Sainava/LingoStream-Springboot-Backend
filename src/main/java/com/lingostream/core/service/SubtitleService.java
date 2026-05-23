package com.lingostream.core.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.lingostream.core.entity.SubtitleEntity;
import com.lingostream.core.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;
    private final RedissonClient redissonClient; // Redis Client
    private final SimpMessagingTemplate messagingTemplate;

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

    @Cacheable(value = "subtitles", key = "#videoId + '_' + #languageCode")
    public List<SubtitleEntity> getSubtitles(String videoId, String languageCode) {
        // This shows that Redis was empty and PostgreSQL had to do the work!
        log.warn("CACHE MISS: Fetching {} [{}] from PostgreSQL database...", videoId, languageCode);

        return subtitleRepository.findByVideoIdAndLanguageCodeOrderByStartTimeMsAsc(videoId, languageCode);
    }

    @CacheEvict(value = "subtitles", key = "#result.videoId + '_' + #result.languageCode")
    @Transactional
    public SubtitleEntity updateSubtitle(UUID id, String newContent) {
        // Create a highly specific lock just for this one subtitle row
        String lockKey = "lock:subtitle:" + id.toString();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Wait up to 5 seconds to get the lock. If acquired, hold it for a max of 10 seconds.
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("Collision detected! Could not acquire lock for subtitle: {}", id);
                throw new RuntimeException("Another translator is currently editing this line. Please try again.");
            }

            log.info("Lock acquired for subtitle {}. Updating content...", id);

            // Fetch, update, and save
            SubtitleEntity subtitle = subtitleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subtitle not found"));

            subtitle.setContent(newContent);
            SubtitleEntity savedSubtitle = subtitleRepository.save(subtitle);

            // --- THE WEBSOCKET BROADCAST ---
            // Shout the new subtitle data to anyone subscribed to this specific video's channel
            String destination = "/topic/subtitles/" + savedSubtitle.getVideoId();
            messagingTemplate.convertAndSend(destination, savedSubtitle);
            log.info("Broadcasted live update to {}", destination);

            return savedSubtitle;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("System interrupted while waiting for lock", e);
        } finally {
            // CRITICAL: Always release the lock so other users aren't frozen out forever
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock released for subtitle {}", id);
            }
        }
    }
}