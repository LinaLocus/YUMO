package com.voicenotes.controller;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.dto.TranscriptionDtos.*;
import com.voicenotes.security.CurrentUser;
import com.voicenotes.service.TranscriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {
    private final TranscriptionService service;
    private final CurrentUser currentUser;

    public TranscriptionController(TranscriptionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "template", required = false) SummaryTemplate template) {
        Long uid = currentUser.requireUserId();
        Transcription t = service.upload(uid, file, template);
        return new UploadResponse(t.getId(), t.getStatus());
    }

    @GetMapping("/{id}")
    public DetailResponse detail(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        return DetailResponse.from(service.getOwned(uid, id));
    }

    @GetMapping
    public List<ListItem> list() {
        Long uid = currentUser.requireUserId();
        return service.list(uid).stream().map(ListItem::from).toList();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        service.delete(uid, id);
    }
}
