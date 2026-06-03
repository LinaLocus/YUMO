package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class TtsServiceTest {
    AppProperties props;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getTts().setBaseUrl("http://relay.example/v1");
        props.getTts().setApiKey("test-key");
        props.getTts().setMaxChars(10);
    }

    @Test
    void stripsMarkdownToPlainText() {
        TtsService svc = new TtsService(props);
        String plain = svc.toPlainText("# 标题\n- **要点**一\n- 要点二\n`code`");
        assertThat(plain).doesNotContain("#").doesNotContain("*").doesNotContain("`");
        assertThat(plain).contains("标题").contains("要点一").contains("要点二");
    }

    @Test
    void splitsByMaxChars() {
        TtsService svc = new TtsService(props);
        List<String> parts = svc.splitText("一二三四五六七八九十一二三四五"); // 15 chars, max 10
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).length()).isLessThanOrEqualTo(10);
    }

    @Test
    void splitsAtSentenceBoundary() {
        props.getTts().setMaxChars(10);
        TtsService svc = new TtsService(props);
        // 第一句 6 字 + 句号在 max/2 之后，应在句号处自然断开
        List<String> parts = svc.splitText("第一句话内容。第二句话内容。");
        assertThat(parts.get(0)).endsWith("。");
        assertThat(parts.get(0)).isEqualTo("第一句话内容。");
    }

    @Test
    void synthesizeConcatenatesSegmentBytes() {
        List<String> calls = new ArrayList<>();
        List<String> voices = new ArrayList<>();
        TtsService svc = new TtsService(props) {
            @Override protected byte[] requestSpeech(String text, String voiceId) {
                calls.add(text);
                voices.add(voiceId);
                return text.getBytes();
            }
        };
        props.getTts().setMaxChars(5);
        props.getTts().setVoice("default-voice");
        byte[] out = svc.synthesize("一二三四五六七", null); // voice 为空 -> 用默认
        assertThat(calls).hasSize(2);
        assertThat(voices).containsOnly("default-voice");
        assertThat(new String(out)).isEqualTo("一二三四五六七");
    }

    @Test
    void synthesizeUsesProvidedVoice() {
        List<String> voices = new ArrayList<>();
        TtsService svc = new TtsService(props) {
            @Override protected byte[] requestSpeech(String text, String voiceId) {
                voices.add(voiceId);
                return text.getBytes();
            }
        };
        svc.synthesize("文本", "female-yujie");
        assertThat(voices).containsOnly("female-yujie");
    }

    @Test
    void missingConfigThrows() {
        props.getTts().setApiKey("");
        TtsService svc = new TtsService(props);
        assertThatThrownBy(() -> svc.synthesize("文本", null))
                .isInstanceOf(ApiException.class);
    }
}
