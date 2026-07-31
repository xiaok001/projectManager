package com.pm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pm.model.entity.SysUser;
import com.pm.model.dto.LoginDTO;
import com.pm.model.vo.LoginVO;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    LoginVO login(LoginDTO dto);
    List<SysUser> listAllUsers();
    SysUser getByUsername(String username);
}
