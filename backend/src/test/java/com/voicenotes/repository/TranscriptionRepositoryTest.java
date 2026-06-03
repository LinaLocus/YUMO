package com.voicenotes.repository;

import com.voicenotes.domain.Transcription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TranscriptionRepositoryTest {
    @Autowired TranscriptionRepository repo;

    @Test
    void findsByUserIdOrdered() {
        Transcription a = make(1L, "a.mp3");
        Transcription b = make(1L, "b.mp3");
        Transcription other = make(2L, "c.mp3");
        repo.saveAll(List.of(a, b, other));

        List<Transcription> mine = repo.findByUserIdOrderByCreatedAtDesc(1L);
        assertThat(mine).hasSize(2);

        assertThat(repo.findByIdAndUserId(other.getId(), 1L)).isEmpty();
        assertThat(repo.findByIdAndUserId(other.getId(), 2L)).isPresent();
    }

    private Transcription make(Long userId, String name) {
        Transcription t = new Transcription();
        t.setUserId(userId);
        t.setOriginalFilename(name);
        t.setAudioPath("/tmp/" + name);
        return t;
    }
}
