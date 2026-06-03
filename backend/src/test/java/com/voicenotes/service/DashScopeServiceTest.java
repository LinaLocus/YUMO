package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class DashScopeServiceTest {
    AppProperties props;
    @TempDir Path tmp;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getDashscope().setApiKey("test-key");
    }

    @Test
    void transcribeReturnsStubbedText() throws Exception {
        Path audio = tmp.resolve("a.mp3");
        Files.writeString(audio, "x");
        DashScopeService svc = new DashScopeService(props) {
            @Override protected String doRecognize(String audioPath) { return "你好 世界"; }
        };
        assertThat(svc.transcribe(audio.toString())).isEqualTo("你好 世界");
    }

    @Test
    void missingKeyThrows() {
        props.getDashscope().setApiKey("");
        DashScopeService svc = new DashScopeService(props);
        assertThatThrownBy(() -> svc.transcribe("whatever.mp3"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DASHSCOPE_API_KEY");
    }

    @Test
    void missingFileThrows() {
        DashScopeService svc = new DashScopeService(props);
        assertThatThrownBy(() -> svc.transcribe(tmp.resolve("nope.mp3").toString()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void detectFormat() {
        DashScopeService svc = new DashScopeService(props);
        assertThat(svc.detectFormat("/x/a.WAV")).isEqualTo("wav");
        assertThat(svc.detectFormat("/x/a.m4a")).isEqualTo("m4a");
        assertThat(svc.detectFormat("/x/a.mp3")).isEqualTo("mp3");
    }

    @Test
    void extractPlainTextFromSentencesJson() {
        DashScopeService svc = new DashScopeService(props);
        String raw = "{\"sentences\":[{\"text\":\"第一句。\"},{\"text\":\"第二句。\"}]}";
        assertThat(svc.extractPlainText(raw)).isEqualTo("第一句。第二句。");
    }

    @Test
    void extractPlainTextPassesThroughPlain() {
        DashScopeService svc = new DashScopeService(props);
        assertThat(svc.extractPlainText("已经是纯文本")).isEqualTo("已经是纯文本");
    }
}
