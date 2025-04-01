package ru.yandex.practicum.filmorate.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.FriendsException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public void addFriend(int userId, int friendId) {
        if (userId == friendId) {
            log.error("Attempt to add self as a friend: userId={}, friendId={}", userId, friendId);
            throw new FriendsException("Cannot add/remove self as a friend");
        }

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.addFriend(friendId);
        friend.addFriend(userId);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("User {} added user {} as a friend", userId, friendId);
    }

    public void deleteFriend(int userId, int friendId) {
        if (userId == friendId) {
            log.error("Attempt to remove self as a friend: userId={}, friendId={}", userId, friendId);
            throw new FriendsException("Cannot add/remove self as a friend");
        }

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.removeFriend(friendId);
        friend.removeFriend(userId);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("User {} removed user {} from friends", userId, friendId);
    }

    public Set<User> getMutualFriends(int userId, int otherId) {
        User user1 = getUserById(userId);
        User user2 = getUserById(otherId);

        log.info("Getting mutual friends of users {} and {}", userId, otherId);
        return user1.getFriendsList().stream()
                .filter(user2.getFriendsList()::contains)
                .map(userStorage::getUserById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    public Set<User> getFriends(int userId) {
        User user = getUserById(userId);
        log.info("Getting friends list for user {}", userId);
        return user.getFriendsList().stream()
                .map(userStorage::getUserById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    public Collection<User> getAllUsers() {
        log.info("Getting all users");
        return userStorage.getAllUsers();
    }

    public User create(User user) {
        validateUser(user);
        User createdUser = userStorage.create(user);
        log.info("Created user with ID: {}", createdUser.getId());
        return createdUser;
    }

    public User update(User user) {
        getUserById(user.getId());
        validateUser(user);
        User updatedUser = userStorage.update(user);
        log.info("Updated user with ID: {}", updatedUser.getId());
        return updatedUser;
    }


    private User getUserById(int userId) {
        return userStorage.getUserById(userId).orElseThrow(() -> {
            log.error("User with ID {} not found", userId);
            return new NotFoundException("User with ID " + userId + " not found");
        });
    }

    private void validateUser(User user) {
        if (user == null) {
            log.error("Null user provided");
            throw new ValidationException("User cannot be null");
        }
        if (!isValidEmail(user.getEmail())) {
            log.error("Invalid email: {}", user.getEmail());
            throw new ValidationException("Invalid email");
        }
        if (!isValidLogin(user.getLogin())) {
            log.error("Invalid login: {}", user.getLogin());
            throw new ValidationException("Invalid login");
        }
        if (!isValidBirthday(user.getBirthday())) {
            log.error("Invalid birthday: {}", user.getBirthday());
            throw new ValidationException("Invalid birthday");
        }
    }


    private boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    private boolean isValidLogin(String login) {
        return login != null && !login.contains(" ");
    }

    private boolean isValidBirthday(LocalDate birthday) {
        return birthday != null && birthday.isBefore(LocalDate.now());
    }
}