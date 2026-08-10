package com.api.distr.docs.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── RowMapper ────────────────────────────────────────────────────────────

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setEnabled(rs.getBoolean("enabled"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        u.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        u.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        return u;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<User> findAll() {
        return jdbc.query(
            "SELECT * FROM users ORDER BY created_at DESC",
            USER_ROW_MAPPER
        );
    }

    public Optional<User> findById(Long id) {
        List<User> result = jdbc.query(
            "SELECT * FROM users WHERE id = ?",
            USER_ROW_MAPPER, id
        );
        return result.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        List<User> result = jdbc.query(
            "SELECT * FROM users WHERE username = ?",
            USER_ROW_MAPPER, username
        );
        return result.stream().findFirst();
    }

    public Optional<User> findByPhone(String phone) {
        List<User> result = jdbc.query(
            "SELECT * FROM users WHERE phone = ?",
            USER_ROW_MAPPER, phone
        );
        return result.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?",
            Integer.class, username
        );
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?",
            Integer.class, email
        );
        return count != null && count > 0;
    }

    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id = ?",
            Integer.class, id
        );
        return count != null && count > 0;
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public User save(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            // PostgreSQL requires explicit column name to return the generated key reliably
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (username, password, full_name, email, phone, role, enabled, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new String[]{"id"}
            );
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.setBoolean(7, user.isEnabled());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        long id = Objects.requireNonNull(keyHolder.getKey(), "INSERT did not return generated id").longValue();
        return findById(id).orElseThrow();
    }

    public User update(User user) {
        jdbc.update(
            "UPDATE users SET full_name = ?, email = ?, phone = ?, role = ?, enabled = ?, password = ?, updated_at = ? WHERE id = ?",
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name(),
            user.isEnabled(),
            user.getPassword(),
            Timestamp.valueOf(LocalDateTime.now()),
            user.getId()
        );
        return findById(user.getId()).orElseThrow();
    }

    public void updatePasswordById(Long id, String encodedPassword) {
        jdbc.update(
            "UPDATE users SET password = ?, updated_at = ? WHERE id = ?",
            encodedPassword,
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM users WHERE id = ?", id);
    }
}
