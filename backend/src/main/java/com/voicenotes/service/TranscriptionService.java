package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TranscriptionService {
    private final TranscriptionRepository repo;
    private final StorageService storage;
    private final DashScopeService dashScope;
    private final TtsService tts;

    public TranscriptionService(TranscriptionRepository repo, StorageService storage,
                                DashScopeService dashScope, TtsService tts) {
        this.repo = repo;
        this.storage = storage;
        this.dashScope = dashScope;
        this.tts = tts;
    }

    /** 按所选音色生成朗读音频（每次重新生成），返回更新后的记录。voice 为空则用默认音色。 */
    public Transcription generateSpeech(Long userId, Long id, String voice) {
        Transcription t = getOwned(userId, id);
        if (t.getSummaryMarkdown() == null || t.getSummaryMarkdown().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "尚无概括内容，无法朗读");
        }
        // 删除旧音频，避免换音色后仍播放旧文件
        if (t.getTtsAudioPath() != null) {
            storage.delete(t.getTtsAudioPath());
        }
        byte[] audio = tts.synthesize(t.getSummaryMarkdown(), voice);
        String path = storage.saveTts(t.getId(), audio);
        t.setTtsAudioPath(path);
        return repo.save(t);
    }

    // 新增：触发转写（同步执行核心逻辑，便于测试）
    public void runTranscription(Long userId, Long id) {
        Transcription t = getOwned(userId, id);
        if (t.getStatus() == TranscriptionStatus.TRANSCRIBING) {
            throw new ApiException(HttpStatus.CONFLICT, "正在转写中");
        }
        t.setStatus(TranscriptionStatus.TRANSCRIBING);
        t.setErrorMessage(null);
        repo.save(t);
        try {
            String text = dashScope.transcribe(t.getAudioPath());
            t.setTranscriptText(text);
            t.setStatus(TranscriptionStatus.TRANSCRIBED);
            repo.save(t);
        } catch (Exception e) {
            t.setStatus(TranscriptionStatus.FAILED);
            t.setErrorMessage(e.getMessage());
            repo.save(t);
        }
    }

    public Transcription upload(Long userId, MultipartFile file, SummaryTemplate template) {
        String path = storage.saveAudio(file);
        Transcription t = new Transcription();
        t.setUserId(userId);
        t.setOriginalFilename(file.getOriginalFilename());
        t.setAudioPath(path);
        t.setTemplate(template == null ? SummaryTemplate.GENERAL : template);
        t.setStatus(TranscriptionStatus.UPLOADED);
        return repo.save(t);
    }

    public Transcription getOwned(Long userId, Long id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权访问或记录不存在"));
    }

    public List<Transcription> list(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(Long userId, Long id) {
        Transcription t = getOwned(userId, id);
        storage.delete(t.getAudioPath());
        storage.delete(t.getTtsAudioPath());
        repo.delete(t);
    }

    public Transcription save(Transcription t) {
        return repo.save(t);
    }
}
