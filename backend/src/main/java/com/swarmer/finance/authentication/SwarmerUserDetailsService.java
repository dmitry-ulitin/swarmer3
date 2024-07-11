package com.swarmer.finance.authentication;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.swarmer.finance.repositories.UserRepository;

@Service
public class SwarmerUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

    public SwarmerUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username).orElse(null);
    }
}
