package ru.yandex.practicum.filmorate.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exceptions.ContentNotException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Transactional
    public User addFriends(int userId1, int userId2) {
        log.info("UserService: received request to add friend. User1 ID: {}, User2 ID: {}", userId1, userId2);
        User user1 = findUserById(userId1);
        User user2 = findUserById(userId2);

        if (user1.getFriends().contains(user2)) {
            log.warn("UserService: User {} is already friends with User {}", userId1, userId2);
            throw new ValidationException("User " + userId2 + " is already added as a friend");
        }

        userStorage.addFriend(userId1, userId2);
        user1.getFriends().add(user2);
        log.info("UserService: User {} and User {} are now friends", userId1, userId2);
        return user1;
    }

    @Transactional
    public User deleteFriends(int userId1, int userId2) {
        log.info("UserService: received request to delete friend. User1 ID: {}, User2 ID: {}", userId1, userId2);
        User user1 = findUserById(userId1);
        User user2 = findUserById(userId2);

        if (!user1.getFriends().contains(user2)) {
            log.warn("UserService: User {} is not friends with User {}", userId1, userId2);
            throw new ContentNotException("User with ID " + userId2 + " not found in friends list of user with ID " + userId1);
        }

        userStorage.removeFriend(userId1, userId2);
        user1.getFriends().remove(user2);
        log.info("UserService: User {} and User {} are no longer friends", userId1, userId2);
        return user1;
    }

    public Set<User> getCommonFriends(int userId1, int userId2) {
        log.info("UserService: received request to get common friends for User1 ID: {}, User2 ID: {}", userId1, userId2);
        findUserById(userId1);
        findUserById(userId2);

        Set<User> commonFriends = userStorage.getCommonFriends(userId1, userId2);
        log.info("UserService: found {} common friends for User {} and User {}", commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    @Transactional
    public User addUser(User user) {
        log.info("UserService: received request to add user: {}", user.getLogin());
        validateUser(user);
        User addedUser = userStorage.addUser(user);
        log.info("UserService: user created with ID: {}", addedUser.getId());
        return addedUser;
    }

    @Transactional
    public User updateUser(User user) {
        log.info("UserService: received request to update user with ID: {}", user.getId());
        findUserById(user.getId());

        validateUser(user);

        User updatedUser = userStorage.updateUser(user);
        log.info("UserService: user with ID {} updated successfully", updatedUser.getId());
        return updatedUser;
    }

    public Collection<User> getUsers() {
        log.info("UserService: received request to get all users");
        Collection<User> users = userStorage.getUsers();
        log.info("UserService: returning {} users", users.size());
        return users;
    }

    public Collection<User> getFriends(int id) {
        log.info("UserService: received request to get friends for user ID: {}", id);
        User user = findUserById(id);

        Collection<User> friends = user.getFriends();
        log.info("UserService: returning {} friends for user ID {}", friends.size(), id);
        return friends;
    }

    private User findUserById(int userId) {
        log.debug("UserService: Looking for user with ID: {}", userId);
        return userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("UserService: User with ID {} not found", userId);
                    return new NotFoundException("User with ID " + userId + " not found");
                });
    }

    private void validateUser(User user) {
        log.debug("UserService: Validating user: {}", user.getLogin());
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("UserService: User name is blank, setting name to login: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("UserService: Validation failed: Invalid email address for user {}", user.getLogin());
            throw new ValidationException("Invalid email address");
        }
        if (user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error("UserService: Validation failed: Login is empty or contains spaces for user {}", user.getLogin());
            throw new ValidationException("Login is empty or contains spaces");
        }
        if (user.getBirthday() == null) {
            log.error("UserService: Validation failed: Date of birth is not specified for user {}", user.getLogin());
            throw new ValidationException("Date of birth is not specified");
        } else if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("UserService: Validation failed: Invalid date of birth {} for user {}", user.getBirthday(), user.getLogin());
            throw new ValidationException("Invalid date of birth");
        }
        log.debug("UserService: User validation successful for user {}", user.getLogin());
    }
}