package com.voicenotes.dto;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;

import java.time.Instant;

public class TranscriptionDtos {
    public record DetailResponse(
            Long id,
            String originalFilename,
            TranscriptionStatus status,
            String transcriptText,
            String summaryMarkdown,
            boolean hasTtsAudio,
            SummaryTemplate template,
            String errorMessage,
            Instant createdAt) {
        public static DetailResponse from(Transcription t) {
            return new DetailResponse(
                    t.getId(), t.getOriginalFilename(), t.getStatus(),
                    t.getTranscriptText(), t.getSummaryMarkdown(),
                    t.getTtsAudioPath() != null,
                    t.getTemplate(), t.getErrorMessage(), t.getCreatedAt());
        }
    }

    public record ListItem(
            Long id, String originalFilename, TranscriptionStatus status,
            SummaryTemplate template, Instant createdAt) {
        public static ListItem from(Transcription t) {
            return new ListItem(t.getId(), t.getOriginalFilename(), t.getStatus(),
                    t.getTemplate(), t.getCreatedAt());
        }
    }

    public record UploadResponse(Long id, TranscriptionStatus status) {}
}
