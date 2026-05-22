package com.lingostream.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "subtitles", indexes = {
        // We index these because our queries will constantly search by video and time
        @Index(name = "idx_video_lang", columnList = "video_id, language_code"),
        @Index(name = "idx_start_time", columnList = "start_time_ms")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "start_time_ms", nullable = false)
    private Long startTimeMs;

    @Column(name = "end_time_ms", nullable = false)
    private Long endTimeMs;

    // Use columnDefinition = "TEXT" because subtitles can sometimes be very long blocks
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // This is the secret weapon for Phase 2. It tracks edits to prevent data corruption.
    @Version
    private Integer version;
}