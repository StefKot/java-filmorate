package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {
    private static final LocalDate MIN_BIRTHDATE = LocalDate.of(1910, 1, 1);
    private final Map<Integer, User> users = new HashMap<>();
    private int currentId = 0;

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Retrieving all users. Total count: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validateUser(user);
        assignNameToLoginIfEmpty(user);

        user.setId(++currentId);
        users.put(user.getId(), user);
        log.info("User created successfully. ID: {}", user.getId());
        return user;
    }

    @PutMapping
    public User update(@RequestBody User updatedUser) {
        validateUser(updatedUser);

        if (!users.containsKey(updatedUser.getId())) {
            log.error("Attempt to update non-existent user. ID: {}", updatedUser.getId());
            throw new ValidationException("User with ID " + updatedUser.getId() + " not found.");
        }
        assignNameToLoginIfEmpty(updatedUser);

        users.put(updatedUser.getId(), updatedUser);
        log.info("User updated successfully. ID: {}", updatedUser.getId());
        return updatedUser;
    }

    private void validateUser(User user) {
        if (!StringUtils.hasText(user.getEmail()) || !user.getEmail().contains("@")) {
            log.error("User email validation failed. Email: {}", user.getEmail());
            throw new ValidationException("Invalid email format.");
        }

        if (!StringUtils.hasText(user.getLogin()) || user.getLogin().contains(" ")) {
            log.error("User login validation failed. Login: {}", user.getLogin());
            throw new ValidationException("Login cannot be blank or contain spaces.");
        }

        if (user.getBirthday() == null || user.getBirthday().isBefore(MIN_BIRTHDATE) || user.getBirthday().isAfter(LocalDate.now())) {
            log.error("User birthday validation failed. Birthday: {}", user.getBirthday());
            throw new ValidationException("Birthday cannot be null, before " + MIN_BIRTHDATE + ", or in the future.");
        }
    }

    private void assignNameToLoginIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}