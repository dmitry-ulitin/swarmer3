package com.swarmer.finance.security;

import org.springframework.security.core.userdetails.UserDetails;

import com.swarmer.finance.dto.UserDto;
import com.swarmer.finance.models.User;

import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class UserPrincipal implements UserDetails {
    private UserDto userDto;
    private String password;
    private boolean enabled;

    public UserPrincipal(User user) {
        this.userDto = UserDto.fromEntity(user);
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
    }

    public UserPrincipal(UserDto dto) {
        this.userDto = dto;
        this.password = null;
        this.enabled = true;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return userDto.email();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
