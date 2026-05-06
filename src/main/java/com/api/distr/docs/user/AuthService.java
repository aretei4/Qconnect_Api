package com.api.distr.docs.user;

import com.api.distr.docs.security.UserJwtService;
import com.api.distr.docs.user.dto.LoginRequest;
import com.api.distr.docs.user.dto.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final UserJwtService    userJwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserJwtService userJwtService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userJwtService  = userJwtService;
    }

    public LoginResponse login(LoginRequest req) {

        // 1 — find user (same generic message to avoid username enumeration)
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // 2 — verify password
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // 3 — check account is active
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // 4 — issue JWT
        String token = userJwtService.generateToken(
                user.getUsername(), "ROLE_" + user.getRole().name());

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}
