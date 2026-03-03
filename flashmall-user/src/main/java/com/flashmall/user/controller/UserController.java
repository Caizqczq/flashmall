package com.flashmall.user.controller;

import com.flashmall.common.result.Result;
import com.flashmall.common.utils.JwtUtil;
import com.flashmall.user.dto.UserLoginDTO;
import com.flashmall.user.dto.UserRegisterDTO;
import com.flashmall.user.entity.User;
import com.flashmall.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginDTO dto) {
        String token = userService.login(dto);
        return Result.ok("登录成功", token);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token.replace("Bearer ", ""));
        User user = userService.getUserInfo(userId);
        return Result.ok(user);
    }

    @Operation(summary = "根据ID获取用户信息（内部调用）")
    @GetMapping("/inner/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserInfo(id);
        return Result.ok(user);
    }
}
