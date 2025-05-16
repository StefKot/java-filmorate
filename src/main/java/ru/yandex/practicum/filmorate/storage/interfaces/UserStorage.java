package ru.yandex.practicum.filmorate.storage.interfaces;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface UserStorage {
    User addUser(User user);

    User updateUser(User user);

    Collection<User> getUsers();

    Optional<User> getUserById(int id);

    Set<User> getCommonFriends(int userId1, int userId2);

    void removeFriend(int userId, int friendId);

    void addFriend(int userId, int friendId);
}