package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    @EqualsAndHashCode.Include
    private int id;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @EqualsAndHashCode.Exclude
    private String email;

    @NotBlank(message = "Login cannot be blank")
    @Pattern(regexp = "^\\S*$", message = "Login cannot contain spaces")
    @EqualsAndHashCode.Exclude
    private String login;

    @EqualsAndHashCode.Exclude
    private String name;

    @PastOrPresent(message = "Birthday cannot be in the future")
    @EqualsAndHashCode.Exclude
    private LocalDate birthday;

    @EqualsAndHashCode.Exclude
    private Set<Integer> friendsList = new HashSet<>();

    public void addFriend(int friendId) {
        friendsList.add(friendId);
    }

    public void removeFriend(int friendId) {
        friendsList.remove(friendId);
    }
}
