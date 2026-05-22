package com.lingostream.core.controller;

import com.lingostream.core.entity.SubtitleEntity;
import com.lingostream.core.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subtitles")
@RequiredArgsConstructor
public class SubtitleController {

    private final SubtitleRepository subtitleRepository;

    @GetMapping("/{videoId}")
    public ResponseEntity<List<SubtitleEntity>> getSubtitles(
            @PathVariable String videoId,
            @RequestParam(name = "language", defaultValue = "en") String languageCode) {

        // We use the custom query method we defined in the Repository earlier
        List<SubtitleEntity> subtitles = subtitleRepository
                .findByVideoIdAndLanguageCodeOrderByStartTimeMsAsc(videoId, languageCode);

        if (subtitles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(subtitles);
    }
}