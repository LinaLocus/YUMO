package com.voicenotes.controller;

import com.voicenotes.domain.Transcription;
import com.voicenotes.dto.SpeechDtos.SpeechResponse;
import com.voicenotes.security.CurrentUser;
import com.voicenotes.service.SummaryService;
import com.voicenotes.service.TranscriptionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {
    private final SummaryService summaryService;
    private final CurrentUser currentUser;
    private final TranscriptionService transcriptionService;

    public SummaryController(SummaryService summaryService, CurrentUser currentUser,
                             TranscriptionService transcriptionService) {
        this.summaryService = summaryService;
        this.currentUser = currentUser;
        this.transcriptionService = transcriptionService;
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                summaryService.streamSummary(uid, id, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        });
        return emitter;
    }

    @PostMapping("/{id}/speech")
    public SpeechResponse generateSpeech(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        transcriptionService.generateSpeech(uid, id);
        return new SpeechResponse(id, "/api/summaries/" + id + "/speech");
    }

    @GetMapping("/{id}/speech")
    public ResponseEntity<FileSystemResource> playSpeech(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        Transcription t = transcriptionService.getOwned(uid, id);
        if (t.getTtsAudioPath() == null) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource res = new FileSystemResource(t.getTtsAudioPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"summary-" + id + ".mp3\"")
                .body(res);
    }
}
