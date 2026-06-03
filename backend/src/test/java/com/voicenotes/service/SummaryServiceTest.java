package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SummaryServiceTest {
    TranscriptionRepository repo;
    DashScopeService dash;
    PromptTemplateService prompts;
    SummaryService service;

    @BeforeEach
    void setup() {
        repo = mock(TranscriptionRepository.class);
        dash = mock(DashScopeService.class);
        prompts = new PromptTemplateService();
        service = new SummaryService(repo, dash, prompts);
        when(repo.save(any(Transcription.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Transcription transcribed() {
        Transcription t = new Transcription();
        t.setId(7L); t.setUserId(1L);
        t.setStatus(TranscriptionStatus.TRANSCRIBED);
        t.setTranscriptText("会议内容");
        t.setTemplate(SummaryTemplate.MEETING);
        when(repo.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void streamsAndPersistsSummary() {
        Transcription t = transcribed();
        when(dash.streamSummary(anyString(), any())).thenAnswer(inv -> {
            Consumer<String> cb = inv.getArgument(1);
            cb.accept("# 会议纪要\n");
            cb.accept("## 核心结论");
            return "# 会议纪要\n## 核心结论";
        });

        List<String> chunks = new ArrayList<>();
        service.streamSummary(1L, 7L, chunks::add);

        assertThat(chunks).containsExactly("# 会议纪要\n", "## 核心结论");
        assertThat(t.getSummaryMarkdown()).isEqualTo("# 会议纪要\n## 核心结论");
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.DONE);
    }

    @Test
    void rejectsWhenNotTranscribed() {
        Transcription t = new Transcription();
        t.setId(8L); t.setUserId(1L);
        t.setStatus(TranscriptionStatus.UPLOADED);
        when(repo.findByIdAndUserId(8L, 1L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.streamSummary(1L, 8L, c -> {}))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void failureMarksFailed() {
        Transcription t = transcribed();
        when(dash.streamSummary(anyString(), any())).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> service.streamSummary(1L, 7L, c -> {}))
                .isInstanceOf(RuntimeException.class);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
    }
}
