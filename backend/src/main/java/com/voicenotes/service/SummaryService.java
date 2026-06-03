package com.voicenotes.service;

import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class SummaryService {
    private final TranscriptionRepository repo;
    private final DashScopeService dashScope;
    private final PromptTemplateService prompts;

    public SummaryService(TranscriptionRepository repo, DashScopeService dashScope, PromptTemplateService prompts) {
        this.repo = repo;
        this.dashScope = dashScope;
        this.prompts = prompts;
    }

    /** 流式概括：每片通过 onChunk 交出，完成后入库并置 DONE。失败置 FAILED 并抛出。 */
    public void streamSummary(Long userId, Long id, Consumer<String> onChunk) {
        Transcription t = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权访问或记录不存在"));
        if (t.getTranscriptText() == null || t.getTranscriptText().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "尚未完成转写，无法概括");
        }
        t.setStatus(TranscriptionStatus.SUMMARIZING);
        t.setErrorMessage(null);
        repo.save(t);
        try {
            String prompt = prompts.buildPrompt(t.getTemplate(), t.getTranscriptText());
            String full = dashScope.streamSummary(prompt, onChunk);
            t.setSummaryMarkdown(full);
            t.setStatus(TranscriptionStatus.DONE);
            repo.save(t);
        } catch (RuntimeException e) {
            t.setStatus(TranscriptionStatus.FAILED);
            t.setErrorMessage(e.getMessage());
            repo.save(t);
            throw e;
        }
    }
}
