package com.smbtech.examples.qualitypilot;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pilot")
final class PilotController {

    @GetMapping
    Map<String, String> status() {
        return Map.of("status", "ok");
    }
}
