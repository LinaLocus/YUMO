package com.voicenotes.repository;

import com.voicenotes.domain.Transcription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
    List<Transcription> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Transcription> findByIdAndUserId(Long id, Long userId);
}
