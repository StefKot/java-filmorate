package ru.yandex.practicum.filmorate.storage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    @Getter
    private final Map<Integer, User> users = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public User create(User user) {
        int id = idGenerator.incrementAndGet();
        user.setId(id);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        users.put(id, user);
        log.info("Created user with ID: {}", user.getId());
        log.debug("User: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        int userId = user.getId();
        if (!users.containsKey(userId)) {
            log.error("User with ID {} not found", userId);
            throw new NotFoundException("User with ID " + userId + " not found");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        users.put(userId, user);
        log.info("Updated user with ID: {}", user.getId());
        log.debug("User: {}", user);
        return user;
    }

    @Override
    public Optional<User> getUserById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Collection<User> getAllUsers() {
        return users.values();
    }

}