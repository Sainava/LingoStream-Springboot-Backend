package com.lingostream.core.controller;

import com.lingostream.core.entity.SubtitleEntity;
import com.lingostream.core.repository.SubtitleRepository;
import com.lingostream.core.service.SubtitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subtitles")
@RequiredArgsConstructor
public class SubtitleController {

    private final SubtitleRepository subtitleRepository;
    private final SubtitleService subtitleService;

    @GetMapping("/{videoId}")
    public ResponseEntity<List<SubtitleEntity>> getSubtitles(
            @PathVariable String videoId,
            @RequestParam(name = "language", defaultValue = "en") String languageCode) {

        List<SubtitleEntity> subtitles = subtitleRepository
                .findByVideoIdAndLanguageCodeOrderByStartTimeMsAsc(videoId, languageCode);

        if (subtitles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(subtitles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubtitle(
            @PathVariable UUID id,
            @RequestBody String newContent) {
        try {
            SubtitleEntity updated = subtitleService.updateSubtitle(id, newContent);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            // If the lock is held, we return a 409 Conflict
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}