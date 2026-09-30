package com.finanscore.motorscoring.presentation.publicweb;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.TimeUnit;

@Controller
public class LegalPagesController {

    @GetMapping(value = "/terms", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> terms() {
        return legalPage("legal/terms.html");
    }

    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> privacy() {
        return legalPage("legal/privacy.html");
    }

    private ResponseEntity<Resource> legalPage(String classpathLocation) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(new ClassPathResource(classpathLocation));
    }
}
