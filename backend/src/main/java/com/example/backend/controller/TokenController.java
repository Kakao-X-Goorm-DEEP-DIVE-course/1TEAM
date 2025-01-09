package com.example.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.backend.service.KisTokenService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TokenController {
    private final KisTokenService kisTokenService;
    
    @GetMapping("/token")
    public String getToken() throws Exception {
        return kisTokenService.getCachedAccessToken();
    }
}
