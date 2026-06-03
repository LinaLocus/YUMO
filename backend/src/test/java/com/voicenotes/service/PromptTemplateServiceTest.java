package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {
    PromptTemplateService svc = new PromptTemplateService();

    @Test
    void meetingPromptMentionsActionItems() {
        String p = svc.buildPrompt(SummaryTemplate.MEETING, "今天讨论了排期");
        assertThat(p).contains("今天讨论了排期");
        assertThat(p).contains("待办");
        assertThat(p).contains("Markdown");
    }

    @Test
    void lecturePromptMentionsKeyPoints() {
        String p = svc.buildPrompt(SummaryTemplate.LECTURE, "讲了傅里叶变换");
        assertThat(p).contains("讲了傅里叶变换");
        assertThat(p).contains("知识点");
    }

    @Test
    void generalPromptFallback() {
        String p = svc.buildPrompt(SummaryTemplate.GENERAL, "随便聊聊");
        assertThat(p).contains("随便聊聊");
        assertThat(p).contains("概括");
    }
}
