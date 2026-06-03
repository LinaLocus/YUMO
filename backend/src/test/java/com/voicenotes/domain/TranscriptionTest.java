package com.voicenotes.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionTest {
    @Test
    void defaultsAreSet() {
        Transcription t = new Transcription();
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.UPLOADED);
        assertThat(t.getTemplate()).isEqualTo(SummaryTemplate.GENERAL);
        assertThat(t.getCreatedAt()).isNotNull();
    }
}
