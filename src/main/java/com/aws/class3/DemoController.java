package com.aws.class3;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP"
        );
    }
}
