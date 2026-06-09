package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/health")
    public String health() {
        return """
                  ___
                 / _ \\
                | (o) |
                 \\___/
                  | |
                 /   \\

                Servico tá ON!
                """;
    }
}
