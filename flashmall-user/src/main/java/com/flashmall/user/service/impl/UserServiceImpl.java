package com.flashmall.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.common.utils.JwtUtil;
import com.flashmall.user.dto.UserLoginDTO;
import com.flashmall.user.dto.UserRegisterDTO;
import com.flashmall.user.entity.User;
import com.flashmall.user.mapper.UserMapper;
import com.flashmall.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public String login(UserLoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));

        if (user == null) {
            throw new BizException(ResultCodeEnum.USER_NOT_EXIST);
        }

        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCodeEnum.PASSWORD_ERROR);
        }

        if (user.getStatus() == 1) {
            throw new BizException(ResultCodeEnum.FORBIDDEN);
        }

        return JwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public void register(UserRegisterDTO dto) {
        long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));

        if (count > 0) {
            throw new BizException(ResultCodeEnum.USER_ALREADY_EXIST);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(0);
        save(user);
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException(ResultCodeEnum.USER_NOT_EXIST);
        }
        user.setPassword(null);
        return user;
    }
}
