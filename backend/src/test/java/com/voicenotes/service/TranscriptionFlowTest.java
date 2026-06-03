package com.voicenotes.service;

import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.repository.TranscriptionRepository;
import org.junit.jupiter.api.*;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TranscriptionFlowTest {
    TranscriptionRepository repo;
    StorageService storage;
    DashScopeService dash;
    TranscriptionService service;

    @BeforeEach
    void setup() {
        repo = mock(TranscriptionRepository.class);
        storage = mock(StorageService.class);
        dash = mock(DashScopeService.class);
        service = new TranscriptionService(repo, storage, dash);
        when(repo.save(any(Transcription.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Transcription owned() {
        Transcription t = new Transcription();
        t.setId(5L); t.setUserId(1L); t.setAudioPath("/x/a.mp3");
        t.setStatus(TranscriptionStatus.UPLOADED);
        when(repo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void successSetsTranscribed() {
        Transcription t = owned();
        when(dash.transcribe("/x/a.mp3")).thenReturn("全文内容");
        service.runTranscription(1L, 5L);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.TRANSCRIBED);
        assertThat(t.getTranscriptText()).isEqualTo("全文内容");
    }

    @Test
    void failureSetsFailedWithMessage() {
        Transcription t = owned();
        when(dash.transcribe("/x/a.mp3")).thenThrow(new RuntimeException("boom"));
        service.runTranscription(1L, 5L);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(t.getErrorMessage()).contains("boom");
    }
}
