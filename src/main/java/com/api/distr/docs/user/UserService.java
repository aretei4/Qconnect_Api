package com.api.distr.docs.user;

import com.api.distr.docs.user.dto.UserRequest;
import com.api.distr.docs.user.dto.UserResponse;
import com.api.distr.docs.user.dto.UserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public UserResponse createUser(UserRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + req.getUsername());

        if (req.getEmail() != null && !req.getEmail().isBlank()
                && userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + req.getEmail());

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        user.setEnabled(true);

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail()    != null) user.setEmail(req.getEmail());
        if (req.getRole()     != null) user.setRole(req.getRole());
        if (req.getEnabled()  != null) user.setEnabled(req.getEnabled());
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));

        return UserResponse.from(userRepository.update(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id))
            throw new IllegalArgumentException("User not found: " + id);
        userRepository.deleteById(id);
    }
}
