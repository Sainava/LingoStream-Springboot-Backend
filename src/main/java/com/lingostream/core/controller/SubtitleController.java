package com.lingostream.core.controller;

import com.lingostream.core.entity.SubtitleEntity;
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

    // Notice we deleted the SubtitleRepository! The Controller now only talks to the Service.
    private final SubtitleService subtitleService;

    @GetMapping("/{videoId}")
    public ResponseEntity<List<SubtitleEntity>> getSubtitles(
            @PathVariable String videoId,
            @RequestParam(name = "language", defaultValue = "en") String languageCode) {

        // This request now hits the Redis Cache interceptor before ever reaching the database
        List<SubtitleEntity> subtitles = subtitleService.getSubtitles(videoId, languageCode);

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
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}