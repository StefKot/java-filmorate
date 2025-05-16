package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@Primary
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User addUser(User user) {
        validate(user);
        String sql = "INSERT INTO Users (login, email, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);
        user.setId(keyHolder.getKey().intValue());
        log.info("User created with id: {}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        validate(user);
        String sql = "UPDATE Users SET login = ?, email = ?, name = ?, birthday = ? WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(sql,
                user.getLogin(),
                user.getEmail(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        if (rowsUpdated == 0) {
            throw new NotFoundException("User not found");
        }
        log.info("User updated with id: {}", user.getId());
        return user;
    }

    @Override
    public Collection<User> getUsers() {
        String sql = "SELECT u.*, " +
                "f.friend_id AS friend_id, " +
                "f.status AS friendship_status, " +
                "uf.login AS friend_login, uf.email AS friend_email, " +
                "uf.name AS friend_name, uf.birthday AS friend_birthday " +
                "FROM Users u " +
                "LEFT JOIN user_friends f ON u.id = f.user_id " +
                "LEFT JOIN Users uf ON f.friend_id = uf.id " +
                "ORDER BY u.id ASC, f.friend_id ASC";

        return jdbcTemplate.query(sql, this::mapRowToUser).stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Override
    public Optional<User> getUserById(int id) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        Optional<User> user = jdbcTemplate.query(sql, this::mapRowToUser, id)
                .stream()
                .findFirst();
        user.ifPresent(this::loadFriends);
        return user;
    }

    private void loadFriends(User user) {
        String sql = "SELECT u.* FROM Users u " +
                "JOIN User_friends uf ON u.id = uf.friend_id " +
                "WHERE uf.user_id = ?";
        user.setFriends(new HashSet<>(jdbcTemplate.query(sql, this::mapRowToUser, user.getId())));
    }

    public void addFriend(int userId, int friendId) {
        String sql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, "CONFIRMED");
    }

    public void removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    public Set<User> getCommonFriends(int userId1, int userId2) {
        String sql = "SELECT u.* " +
                "FROM Users u " +
                "WHERE u.id IN (" +
                "    SELECT uf1.friend_id " +
                "    FROM user_friends uf1 " +
                "    WHERE uf1.user_id = ? " +
                "    AND uf1.friend_id IN (" +
                "        SELECT uf2.friend_id " +
                "        FROM User_friends uf2 " +
                "        WHERE uf2.user_id = ?" +
                "    )" +
                ") " +
                "ORDER BY u.id ASC";
        return new HashSet<>(jdbcTemplate.query(sql, this::mapRowToUser, userId1, userId2));
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    private void validate(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Invalid email address");
        }
        if (user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Login is empty or contains spaces");
        }
        if (user.getBirthday() == null) {
            throw new ValidationException("Date of birth is not specified");
        } else if (user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Invalid date of birth");
        }
    }
}