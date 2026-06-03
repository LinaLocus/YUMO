package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class StorageServiceTest {
    StorageService storage;
    @TempDir Path tmp;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        props.getStorage().setAudioDir(tmp.resolve("audio").toString());
        props.getStorage().setTtsDir(tmp.resolve("tts").toString());
        storage = new StorageService(props);
    }

    @Test
    void savesAudioAndReturnsPath() throws Exception {
        var file = new MockMultipartFile("file", "meeting.mp3", "audio/mpeg", "hello".getBytes());
        String path = storage.saveAudio(file);
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(path).endsWith(".mp3");
    }

    @Test
    void rejectsUnsupportedExtension() {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "x".getBytes());
        assertThatThrownBy(() -> storage.saveAudio(file)).isInstanceOf(Exception.class);
    }

    @Test
    void deleteRemovesFile() throws Exception {
        var file = new MockMultipartFile("file", "a.wav", "audio/wav", "x".getBytes());
        String path = storage.saveAudio(file);
        storage.delete(path);
        assertThat(Files.exists(Path.of(path))).isFalse();
    }

    @Test
    void savesTtsBytes() throws Exception {
        String path = storage.saveTts(1L, new byte[]{1, 2, 3});
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(path).endsWith(".mp3");
    }
}
