package com.example.submito.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Helper {
    @GetMapping("/health")
    fun healthCheck(): String {
        return "Everything is OK"
    }
}
