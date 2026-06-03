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

    public TranscriptionService(TranscriptionRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
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
