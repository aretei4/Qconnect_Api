package com.api.distr.docs.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds / repairs default user accounts on every startup.
 *
 * - If the user does not exist → creates it with a freshly BCrypt-hashed password.
 * - If the user already exists → always re-hashes the default password.
 *   This repairs rows that were inserted with invalid/placeholder hashes.
 *
 * Default credentials (change via PUT /api/users/{id} after first login):
 *   admin   / Admin@123    → ADMIN
 *   manager / Manager@123  → MANAGER
 *   staff   / Staff@123    → STAFF
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        upsertSeedUser("admin",   "Admin@123",   "System Administrator", "admin@device4autism.in",   Role.ADMIN);
        upsertSeedUser("manager", "Manager@123", "Store Manager",        "manager@device4autism.in", Role.MANAGER);
        upsertSeedUser("staff",   "Staff@123",   "Staff Member",         "staff@device4autism.in",   Role.STAFF);
    }

    private void upsertSeedUser(String username, String rawPassword,
                                String fullName, String email, Role role) {

        String encoded = passwordEncoder.encode(rawPassword);

        userRepository.findByUsername(username).ifPresentOrElse(
            existing -> {
                // Always fix the password hash — repairs any invalid placeholder hashes
                userRepository.updatePasswordById(existing.getId(), encoded);
                System.out.println("[DataInitializer] Updated password hash for: " + username);
            },
            () -> {
                User user = new User();
                user.setUsername(username);
                user.setPassword(encoded);
                user.setFullName(fullName);
                user.setEmail(email);
                user.setRole(role);
                user.setEnabled(true);
                userRepository.save(user);
                System.out.println("[DataInitializer] Created seed user: " + username + " (" + role + ")");
            }
        );
    }
}
