package com.swarmer.finance.controllers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swarmer.finance.dto.UserToCreate;
import com.swarmer.finance.dto.LoginCredentials;
import com.swarmer.finance.models.User;
import com.swarmer.finance.repositories.UserRepository;

@RestController
@RequestMapping("/api/auth")
class AuthController {
	private final UserRepository userRepository;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	@Value("${jwt_secret:secret}")
	private String secret;

	AuthController(UserRepository userRepository, AuthenticationManager authenticationManager,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.authenticationManager = authenticationManager;
		this.passwordEncoder = passwordEncoder;
	}

	@PostMapping("/register")
	Map<String, Object> register(@RequestBody UserToCreate userToCreate) {
		String encodedPass = passwordEncoder.encode(userToCreate.password());
		String currency = userToCreate.currency() == null || userToCreate.currency().isBlank() ? "EUR"
				: userToCreate.currency();
		var user = new User(null, userToCreate.email(), encodedPass, true, userToCreate.name(), currency,
				LocalDateTime.now(), LocalDateTime.now(), null);
		user = userRepository.save(user);
		user = userRepository.findById(user.getId()).orElse(null);
		var token = generateToken(user);
		return Collections.singletonMap("token", token);
	}

	@PostMapping("/login")
	Map<String, Object> login(@RequestBody LoginCredentials credentials) {
		try {
			var authInputToken = new UsernamePasswordAuthenticationToken(credentials.getEmail(),
					credentials.getPassword());
			var authentication = authenticationManager.authenticate(authInputToken);
			var user = (User) authentication.getPrincipal();
			var token = generateToken(user);
			return Collections.singletonMap("token", token);
		} catch (Exception e) {
			return Collections.singletonMap("error", e.getMessage());
		}
	}

	private String generateToken(User user) {
		var token = JWT
				.create()
				.withSubject(user.getEmail())
				.withClaim("id", user.getId())
				.withClaim("name", user.getName())
				.withClaim("currency", user.getCurrency())
				.withIssuedAt(new Date())
				.sign(Algorithm.HMAC256(secret));
		return token;
	}
}