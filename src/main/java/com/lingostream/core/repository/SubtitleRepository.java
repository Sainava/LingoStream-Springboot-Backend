package com.lingostream.core.repository;

import com.lingostream.core.entity.SubtitleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubtitleRepository extends JpaRepository<SubtitleEntity, UUID> {

    // Spring Data JPA writes the SQL for this based purely on the method name
    List<SubtitleEntity> findByVideoIdAndLanguageCodeOrderByStartTimeMsAsc(String videoId, String languageCode);
}