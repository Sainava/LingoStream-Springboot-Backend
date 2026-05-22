package com.lingostream.core.service;

import com.lingostream.core.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataIngestionRunner implements CommandLineRunner {

    private final SubtitleService subtitleService;
    private final SubtitleRepository subtitleRepository;

    @Value("classpath:dataset/iron_man_1_en.srt")
    private Resource ironMan1En;

    @Value("classpath:dataset/iron_man_1_ja.srt")
    private Resource ironMan1Ja;

    // Added German Resource definition
    @Value("classpath:dataset/iron_man_1_ge.srt")
    private Resource ironMan1Ge;

    @Override
    public void run(String... args) {
        // Temporarily comment when it is essential to force ingestion of the missing languages
         if (subtitleRepository.count() > 0) {
             log.info("Database is already populated. Skipping ingestion.");
             return;
         }

        log.info("Initiating fresh data ingestion sequence for all languages...");

        try {
            subtitleService.parseAndSaveSrt(ironMan1En, "iron-man-1", "en");
            subtitleService.parseAndSaveSrt(ironMan1Ja, "iron-man-1", "ja");
            subtitleService.parseAndSaveSrt(ironMan1Ge, "iron-man-1", "ge");
            log.info("Data ingestion complete!");
        } catch (Exception e) {
            log.error("Ingestion sequence interrupted", e);
        }
    }
}