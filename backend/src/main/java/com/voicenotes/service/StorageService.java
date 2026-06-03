package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {
    private static final Set<String> ALLOWED = Set.of("mp3", "wav", "m4a");
    private final Path audioDir;
    private final Path ttsDir;

    public StorageService(AppProperties props) {
        this.audioDir = Path.of(props.getStorage().getAudioDir());
        this.ttsDir = Path.of(props.getStorage().getTtsDir());
    }

    public String saveAudio(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无法识别文件类型");
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持 mp3/wav/m4a");
        }
        try {
            Files.createDirectories(audioDir);
            Path target = audioDir.resolve(UUID.randomUUID() + "." + ext);
            file.transferTo(target.toAbsolutePath());
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存音频失败: " + e.getMessage());
        }
    }

    public String saveTts(Long transcriptionId, byte[] audioBytes) {
        try {
            Files.createDirectories(ttsDir);
            Path target = ttsDir.resolve("tts-" + transcriptionId + "-" + UUID.randomUUID() + ".mp3");
            Files.write(target, audioBytes);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存朗读音频失败: " + e.getMessage());
        }
    }

    public void delete(String path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException ignored) {
        }
    }
}
