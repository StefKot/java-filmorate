package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {
    @EqualsAndHashCode.Include
    private int id;

    @NotBlank(message = "Name cannot be blank")
    @EqualsAndHashCode.Exclude
    private String name;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    @EqualsAndHashCode.Exclude
    private String description;

    @NotNull(message = "Release date cannot be null")
    @Past(message = "Release date must be in the past")
    @EqualsAndHashCode.Exclude
    private LocalDate releaseDate;

    @Positive(message = "Duration must be positive")
    @EqualsAndHashCode.Exclude
    private int duration;

    @EqualsAndHashCode.Exclude
    private Set<Integer> likeScore = new HashSet<>();

    public void addLike(int userId) {
        likeScore.add(userId);
    }

    public void removeLike(int userId) {
        likeScore.remove(userId);
    }
}
