package com.flashmall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flashmall.user.dto.UserLoginDTO;
import com.flashmall.user.dto.UserRegisterDTO;
import com.flashmall.user.entity.User;

public interface UserService extends IService<User> {

    String login(UserLoginDTO dto);

    void register(UserRegisterDTO dto);

    User getUserInfo(Long userId);
}
