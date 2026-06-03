package com.voicenotes.controller;

import com.voicenotes.security.CurrentUser;
import com.voicenotes.service.SummaryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {
    private final SummaryService summaryService;
    private final CurrentUser currentUser;

    public SummaryController(SummaryService summaryService, CurrentUser currentUser) {
        this.summaryService = summaryService;
        this.currentUser = currentUser;
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
}
