/*
* Copyright (c)  2018
* All rights reserved.
* 2018-08-21 09:39:51
*/
package com.ueboot.shiro.service.userrole.impl;

import com.ueboot.shiro.entity.Role;
import com.ueboot.shiro.entity.User;
import com.ueboot.shiro.entity.UserRole;
import com.ueboot.core.repository.BaseRepository;
import com.ueboot.shiro.repository.role.RoleRepository;
import com.ueboot.shiro.repository.user.UserRepository;
import com.ueboot.shiro.repository.userrole.UserRoleRepository;
import com.ueboot.core.service.impl.BaseServiceImpl;
import com.ueboot.shiro.service.userrole.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;

/**
 * Created on 2018-08-21 09:39:51
 * @author yangkui
 * @since 2.1.0 by ueboot-generator
 */
@Slf4j
@Service
public class UserRoleServiceImpl extends BaseServiceImpl<UserRole> implements UserRoleService{
    @Resource
    private UserRoleRepository userRoleRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private UserRepository userRepository;

    @Override
    protected BaseRepository getBaseRepository() {
         return userRoleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30, propagation = Propagation.REQUIRED)
    public void saveUserRole(Long userId, Long[] roleIds) {
        //删除原有数据
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        if(!roles.isEmpty()){
            this.userRoleRepository.delete(roles);
        }
        //插入新数据
        User user = userRepository.findById(userId);
        StringBuilder roleNames = new StringBuilder();
        StringBuilder roleIdsStr = new StringBuilder();
        for (int i = 0; i < roleIds.length; i++) {
            Long roleId = roleIds[i];
            Role role = roleRepository.getOne(roleId);
            roleIdsStr.append(roleId).append(",");
            roleNames.append(role.getName()).append(",");
            role.setId(roleId);
            UserRole userRole = new UserRole();
            userRole.setRole(role);
            userRole.setUser(user);
            this.userRoleRepository.save(userRole);
        }
        String roleName = roleNames.toString();
        if(roleName.endsWith(",")){
            roleName = roleName.substring(0,roleName.length()-1);
        }
        user.setRoleNames(roleName);
        user.setRoleIds(roleIdsStr.toString());
        userRepository.save(user);
    }

    /**
     * 根据用户ID查询用户所属角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<UserRole> findByUserId(Long userId) {
        return userRoleRepository.findByUserId(userId);
    }

    /**
     * 根据角色ID查询角色所属用户列表
     * @param roleId 角色ID
     * @return 用户列表
     */
    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        return userRoleRepository.findByRoleId(roleId);
    }
}
