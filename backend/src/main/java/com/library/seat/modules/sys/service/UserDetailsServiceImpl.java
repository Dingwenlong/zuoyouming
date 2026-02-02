package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.mapper.SysUserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, "active"));
        
        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found or banned: " + username);
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (sysUser.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(sysUser.getRole()));
        }

        return new User(sysUser.getUsername(), sysUser.getPassword(), authorities);
    }
    
    public void deductCreditScore(Long userId, int score) {
        SysUser user = this.getById(userId);
        if (user != null) {
            int newScore = Math.max(0, user.getCreditScore() - score);
            user.setCreditScore(newScore);
            this.updateById(user);
        }
    }

    public void addCreditScore(Long userId, int score) {
        SysUser user = this.getById(userId);
        if (user != null) {
            int newScore = Math.min(100, user.getCreditScore() + score);
            user.setCreditScore(newScore);
            this.updateById(user);
        }
    }
}
