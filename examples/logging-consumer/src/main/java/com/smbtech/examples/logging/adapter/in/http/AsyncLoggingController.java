package com.smbtech.examples.logging.adapter.in.http;

import com.smbtech.examples.logging.application.AsyncLoggingScenarioService;
import com.smbtech.examples.logging.application.AsyncLoggingScenarioService.EmissionResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/logging")
public class AsyncLoggingController {
    private static final int MAX_EVENTS = 10_000;

    private final AsyncLoggingScenarioService scenarioService;

    public AsyncLoggingController(AsyncLoggingScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/async")
    public EmissionResult emit(
            @RequestParam(name = "events", defaultValue = "100") int events,
            @RequestParam(name = "critical-every", defaultValue = "10") int criticalEvery) {
        if (events < 1 || events > MAX_EVENTS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "events must be between 1 and " + MAX_EVENTS);
        }
        if (criticalEvery < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "critical-every must be greater than or equal to 0");
        }
        return scenarioService.emit(events, criticalEvery);
    }
}
