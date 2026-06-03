package com.voicenotes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TranscriptionUploadIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String registerAndToken(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode node = om.readTree(json);
        return node.get("token").asText();
    }

    @Test
    void uploadStoresRecord() throws Exception {
        String token = registerAndToken("upUser");
        var file = new MockMultipartFile("file", "meeting.mp3", "audio/mpeg", "fakeaudio".getBytes());

        String resp = mvc.perform(multipart("/api/transcriptions").file(file)
                        .param("template", "MEETING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn().getResponse().getContentAsString();

        long id = om.readTree(resp).get("id").asLong();
        mvc.perform(get("/api/transcriptions/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("meeting.mp3"))
                .andExpect(jsonPath("$.template").value("MEETING"));
    }

    @Test
    void rejectsBadExtension() throws Exception {
        String token = registerAndToken("badExtUser");
        var file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());
        mvc.perform(multipart("/api/transcriptions").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotAccessOthersRecord() throws Exception {
        String tokenA = registerAndToken("ownerUser");
        var file = new MockMultipartFile("file", "a.mp3", "audio/mpeg", "x".getBytes());
        String resp = mvc.perform(multipart("/api/transcriptions").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(resp).get("id").asLong();

        String tokenB = registerAndToken("intruderUser");
        mvc.perform(get("/api/transcriptions/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
