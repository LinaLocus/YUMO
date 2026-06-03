package com.voicenotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicenotes.domain.Transcription;
import com.voicenotes.repository.TranscriptionRepository;
import com.voicenotes.service.TtsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpeechAndDownloadIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TranscriptionRepository repo;
    @MockBean TtsService ttsService;

    private String token(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    /**
     * 通过已认证的上传接口建记录（保证归属当前 token 用户），再用 repo 补 summaryMarkdown 后保存。
     * 这样无论 H2 自增起点为何，记录都归属当前用户，避免越权 403。
     */
    private Long seedDone(String token) throws Exception {
        var file = new MockMultipartFile("file", "a.mp3", "audio/mpeg", "fakeaudio".getBytes());
        String resp = mvc.perform(multipart("/api/transcriptions").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(resp).get("id").asLong();
        Transcription t = repo.findById(id).orElseThrow();
        t.setSummaryMarkdown("# 概括\n- 要点");
        repo.save(t);
        return id;
    }

    @Test
    void generateSpeechThenDownloadMd() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[]{1, 2, 3});
        String tk = token("ttsUser");
        Long id = seedDone(tk);

        mvc.perform(post("/api/summaries/" + id + "/speech")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioUrl").value("/api/summaries/" + id + "/speech"));

        mvc.perform(get("/api/summaries/" + id + "/speech")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"));

        mvc.perform(get("/api/transcriptions/" + id + "/download")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".md")));
    }
}
