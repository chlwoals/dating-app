package com.dating.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/me")
    public String me(@RequestAttribute("email") String email) {
        return "로그인 사용자: " + email;
    }
}