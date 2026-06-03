package com.voicenotes.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transcriptions")
public class Transcription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String audioPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranscriptionStatus status = TranscriptionStatus.UPLOADED;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String transcriptText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summaryMarkdown;

    private String ttsAudioPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryTemplate template = SummaryTemplate.GENERAL;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String v) { this.originalFilename = v; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String v) { this.audioPath = v; }
    public TranscriptionStatus getStatus() { return status; }
    public void setStatus(TranscriptionStatus v) { this.status = v; }
    public String getTranscriptText() { return transcriptText; }
    public void setTranscriptText(String v) { this.transcriptText = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
    public String getTtsAudioPath() { return ttsAudioPath; }
    public void setTtsAudioPath(String v) { this.ttsAudioPath = v; }
    public SummaryTemplate getTemplate() { return template; }
    public void setTemplate(SummaryTemplate v) { this.template = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
