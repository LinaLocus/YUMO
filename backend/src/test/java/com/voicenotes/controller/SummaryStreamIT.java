package com.voicenotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicenotes.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SummaryStreamIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean SummaryService summaryService;

    private String token(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    @Test
    void streamsChunksAndDone() throws Exception {
        doAnswer(inv -> {
            Consumer<String> cb = inv.getArgument(2);
            cb.accept("# 概括\n");
            cb.accept("- 要点");
            return null;
        }).when(summaryService).streamSummary(anyLong(), eq(7L), any());

        String tk = token("sseUser");
        MvcResult res = mvc.perform(get("/api/summaries/7/stream").param("token", tk)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 流式输出由后台线程异步写入响应；轮询等待写完（出现 [DONE]）后再读取已捕获的完整内容。
        // SSE 响应未声明 charset，getContentAsString() 默认按 ISO-8859-1 解码会乱码，需显式按 UTF-8 解码。
        long deadline = System.currentTimeMillis() + 5000;
        while (!res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("[DONE]")
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        String body = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        mvc.perform(asyncDispatch(res))
                .andExpect(status().isOk());

        assertThat(body).contains("# 概括").contains("- 要点").contains("[DONE]");
    }
}
