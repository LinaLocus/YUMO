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
    void synthesizeConcatenatesSegmentBytes() {
        List<String> calls = new ArrayList<>();
        TtsService svc = new TtsService(props) {
            @Override protected byte[] requestSpeech(String text) {
                calls.add(text);
                return text.getBytes();
            }
        };
        props.getTts().setMaxChars(5);
        byte[] out = svc.synthesize("一二三四五六七"); // 7 chars -> 2 段
        assertThat(calls).hasSize(2);
        assertThat(new String(out)).isEqualTo("一二三四五六七");
    }

    @Test
    void missingConfigThrows() {
        props.getTts().setApiKey("");
        TtsService svc = new TtsService(props);
        assertThatThrownBy(() -> svc.synthesize("文本"))
                .isInstanceOf(ApiException.class);
    }
}
