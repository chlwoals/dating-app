package com.dating.backend.controller;

import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public User getMyInfo(@RequestAttribute("email") String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 없음"));
    }
}