package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
@Primary
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User addUser(User user) {
        log.debug("Attempting to add user: {}", user.getLogin());
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
        log.info("UserDbStorage: User created with id: {}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        log.debug("Attempting to update user with ID: {}", user.getId());
        String sql = "UPDATE Users SET login = ?, email = ?, name = ?, birthday = ? WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(sql,
                user.getLogin(),
                user.getEmail(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        if (rowsUpdated == 0) {
            log.error("User with ID {} not found for update", user.getId());
            throw new NotFoundException("User not found");
        }
        log.info("UserDbStorage: User updated with id: {}", user.getId());
        return user;
    }

    @Override
    public Collection<User> getUsers() {
        log.debug("Attempting to retrieve all users with friends from database");
        String sql = "SELECT u.id AS user_id, u.login, u.email, u.name, u.birthday, " +
                "uf.friend_id " +
                "FROM Users u " +
                "LEFT JOIN user_friends uf ON u.id = uf.user_id " +
                "ORDER BY u.id ASC, uf.friend_id ASC";

        Map<Integer, User> userMap = new LinkedHashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Integer userId = rs.getInt("user_id");
            User user = userMap.computeIfAbsent(userId, id -> {
                User newUser = new User();
                try {
                    newUser.setId(id);
                    newUser.setLogin(rs.getString("login"));
                    newUser.setEmail(rs.getString("email"));
                    newUser.setName(rs.getString("name"));
                    Date birthdaySql = rs.getDate("birthday");
                    newUser.setBirthday(birthdaySql != null ? birthdaySql.toLocalDate() : null);
                    newUser.setFriends(new HashSet<>());
                } catch (SQLException e) {
                    log.error("Error mapping user data from ResultSet", e);
                    throw new RuntimeException("Error mapping user data", e);
                }
                return newUser;
            });

            Integer friendId = rs.getInt("friend_id");
            if (!rs.wasNull()) {
                User friend = new User();
                friend.setId(friendId);
                user.getFriends().add(friend);
            }
        });

        log.debug("Finished processing ResultSet for getUsers. Total users found: {}", userMap.size());

        return userMap.values();
    }

    @Override
    public Optional<User> getUserById(int id) {
        log.debug("Attempting to retrieve user by ID {} with friends from database", id);
        String sql = "SELECT u.id AS user_id, u.login, u.email, u.name, u.birthday, " +
                "uf.friend_id " +
                "FROM Users u " +
                "LEFT JOIN user_friends uf ON u.id = uf.user_id " +
                "WHERE u.id = ? " +
                "ORDER BY uf.friend_id ASC";

        User user = jdbcTemplate.query(sql, rs -> {
            User currentUser = null;
            while (rs.next()) {
                if (currentUser == null) {
                    currentUser = new User();
                    try {
                        currentUser.setId(rs.getInt("user_id"));
                        currentUser.setLogin(rs.getString("login"));
                        currentUser.setEmail(rs.getString("email"));
                        currentUser.setName(rs.getString("name"));
                        Date birthdaySql = rs.getDate("birthday");
                        currentUser.setBirthday(birthdaySql != null ? birthdaySql.toLocalDate() : null);
                        currentUser.setFriends(new HashSet<>());
                    } catch (SQLException e) {
                        log.error("Error mapping user data from ResultSet for ID {}", id, e);
                        throw new RuntimeException("Error mapping user data", e);
                    }
                }

                Integer friendId = rs.getInt("friend_id");
                if (!rs.wasNull()) {
                    User friend = new User();
                    friend.setId(friendId);
                    currentUser.getFriends().add(friend);
                }
            }
            return currentUser;
        }, id);

        Optional<User> userOptional = Optional.ofNullable(user);
        log.debug("User with ID {} found: {}", id, userOptional.isPresent());

        return userOptional;
    }

    public void addFriend(int userId, int friendId) {
        log.debug("Attempting to add friend relation: user {} -> friend {}", userId, friendId);
        String sql = "INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, "CONFIRMED");
        log.debug("Friend relation added: user {} -> friend {}", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.debug("Attempting to remove friend relation: user {} -> friend {}", userId, friendId);
        String sql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId, friendId);
        if (rowsDeleted == 0) {
            log.warn("Friend relation not found for removal: user {} -> friend {}", userId, friendId);
        } else {
            log.debug("Friend relation removed: user {} -> friend {}", userId, friendId);
        }
    }

    @Override
    public Set<User> getCommonFriends(int userId1, int userId2) {
        log.debug("Attempting to retrieve common friends for users {} and {}", userId1, userId2);
        String sql = "SELECT u.id, u.login, u.email, u.name, u.birthday " +
                "FROM Users u " +
                "WHERE u.id IN (" +
                "    SELECT uf1.friend_id " +
                "    FROM user_friends uf1 " +
                "    WHERE uf1.user_id = ? " +
                "    INTERSECT " +
                "    SELECT uf2.friend_id " +
                "    FROM user_friends uf2 " +
                "    WHERE uf2.user_id = ?" +
                ") " +
                "ORDER BY u.id ASC";

        Set<User> commonFriends = new HashSet<>(jdbcTemplate.query(sql, this::mapRowToUser, userId1, userId2));
        log.debug("Retrieved {} common friends for users {} and {}", commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setName(rs.getString("name"));
        Date birthdaySql = rs.getDate("birthday");
        user.setBirthday(birthdaySql != null ? birthdaySql.toLocalDate() : null);
        return user;
    }
}