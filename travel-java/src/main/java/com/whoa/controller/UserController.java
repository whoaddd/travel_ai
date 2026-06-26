package com.whoa.controller;

import com.whoa.dto.LoginDTO;
import com.whoa.dto.RegisterDTO;
import com.whoa.entity.User;
import com.whoa.service.UserService;
import com.whoa.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor

public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        String token = userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getNickname()
        );
        return Result.ok(token);
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );
        return Result.ok(token);
    }

    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return Result.ok(user);
    }
}
