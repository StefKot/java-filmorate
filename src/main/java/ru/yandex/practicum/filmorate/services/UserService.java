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
        User user1 = userStorage.getUserById(userId1).orElseThrow(() -> new NotFoundException("User not found"));
        User user2 = userStorage.getUserById(userId2).orElseThrow(() -> new NotFoundException("User not found"));

        if (user1.getFriends().contains(user2)) {
            throw new ValidationException("User " + user2.getId() + " is already added as a friend");
        }

        userStorage.addFriend(userId1, userId2);
        user1.getFriends().add(user2);
        return user1;
    }

    @Transactional
    public User deleteFriends(int userId1, int userId2) {
        User user1 = userStorage.getUserById(userId1).orElseThrow(() -> new NotFoundException("User not found"));
        User user2 = userStorage.getUserById(userId2).orElseThrow(() -> new NotFoundException("User not found"));

        if (!user1.getFriends().contains(user2)) {
            throw new ContentNotException("User " + user2.getId() + " not found in the friends list");
        }

        userStorage.removeFriend(userId1, userId2);
        user1.getFriends().remove(user2);
        return user1;
    }

    public Set<User> getCommonFriends(int userId1, int userId2) {
        userStorage.getUserById(userId1).orElseThrow(() -> new NotFoundException("User not found"));
        userStorage.getUserById(userId2).orElseThrow(() -> new NotFoundException("User not found"));
        return userStorage.getCommonFriends(userId1, userId2);
    }

    public User addUser(User user) {
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        return userStorage.updateUser(user);
    }

    public Collection<User> getUsers() {
        return userStorage.getUsers();
    }

    public Collection<User> getFriends(int id) {
        User user = userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getFriends();
    }
}