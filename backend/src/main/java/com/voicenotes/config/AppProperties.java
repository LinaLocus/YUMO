package com.voicenotes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private DashScope dashscope = new DashScope();
    private Tts tts = new Tts();

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long v) { this.expirationMs = v; }
    }
    public static class Storage {
        private String audioDir;
        private String ttsDir;
        public String getAudioDir() { return audioDir; }
        public void setAudioDir(String v) { this.audioDir = v; }
        public String getTtsDir() { return ttsDir; }
        public void setTtsDir(String v) { this.ttsDir = v; }
    }
    public static class DashScope {
        private String apiKey;
        private String asrModel = "paraformer-realtime-v2";
        private String llmModel = "qwen-plus";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getAsrModel() { return asrModel; }
        public void setAsrModel(String v) { this.asrModel = v; }
        public String getLlmModel() { return llmModel; }
        public void setLlmModel(String v) { this.llmModel = v; }
    }
    public static class Tts {
        private String baseUrl;
        private String apiKey;
        private String model = "speech-2.8-hd";
        private String voice = "alloy";
        private int maxChars = 4000;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public String getVoice() { return voice; }
        public void setVoice(String v) { this.voice = v; }
        public int getMaxChars() { return maxChars; }
        public void setMaxChars(int v) { this.maxChars = v; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt v) { this.jwt = v; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage v) { this.storage = v; }
    public DashScope getDashscope() { return dashscope; }
    public void setDashscope(DashScope v) { this.dashscope = v; }
    public Tts getTts() { return tts; }
    public void setTts(Tts v) { this.tts = v; }
}
