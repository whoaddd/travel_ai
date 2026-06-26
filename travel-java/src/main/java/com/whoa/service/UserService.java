package com.whoa.service;

import com.whoa.entity.User;
import com.whoa.mapper.UserMapper;
import com.whoa.utils.JwtUtils;
import com.whoa.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    // 注册
    public String register(String username, String password, String nickname) {
        // 1. 检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            log.warn("注册失败，用户名已存在: {}", username);
            throw new RuntimeException("用户名已存在");
        }

        // 2. 加密密码
        String hashedPassword = PasswordUtils.hashPassword(password);

        // 3. 插入数据库
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setNickname(nickname != null ? nickname : username);
        userMapper.insert(user);

        log.info("用户注册成功: {}", username);

        // 4. 生成 token 返回
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    // 登录
    public String login(String username, String password) {
        // 1. 查用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("登录失败，用户不存在: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            log.warn("登录失败，密码错误: {}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("用户登录成功: {}", username);

        // 3. 生成 token 返回
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    // 获取用户信息
    public User getUserById(Long userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null); // 返回前清空密码
        }
        return user;
    }
}
