package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;

class DashScopeSummaryTest {
    AppProperties props;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getDashscope().setApiKey("test-key");
    }

    @Test
    void streamsChunksAndReturnsFullText() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) {
                onChunk.accept("# 概");
                onChunk.accept("括\n");
                onChunk.accept("- 要点");
            }
        };
        List<String> received = new ArrayList<>();
        String full = svc.streamSummary("prompt", received::add);
        assertThat(received).containsExactly("# 概", "括\n", "- 要点");
        assertThat(full).isEqualTo("# 概括\n- 要点");
    }

    @Test
    void emptyResultThrows() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) { /* 不发 */ }
        };
        assertThatThrownBy(() -> svc.streamSummary("p", c -> {}))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void underlyingErrorWrapped() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) {
                throw new RuntimeException("network");
            }
        };
        assertThatThrownBy(() -> svc.streamSummary("p", c -> {}))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("概括失败");
    }
}
