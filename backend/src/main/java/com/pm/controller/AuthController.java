package com.pm.controller;

import com.pm.common.response.R;
import com.pm.model.dto.LoginDTO;
import com.pm.model.entity.SysUser;
import com.pm.model.vo.LoginVO;
import com.pm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return R.ok(loginVO);
    }

    @GetMapping("/users")
    public R<List<SysUser>> listUsers() {
        return R.ok(userService.listAllUsers());
    }

    @PostMapping("/verify-username")
    public R<Void> verifyUsername(@RequestBody Map<String, String> body) {
        userService.verifyUsername(body.get("username"));
        return R.ok();
    }

    @PostMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody Map<String, String> body) {
        userService.resetPassword(body.get("username"), body.get("email"));
        return R.ok();
    }
}
