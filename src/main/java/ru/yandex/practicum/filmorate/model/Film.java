package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class Film {
    private int id;

    @NotBlank(message = "Film name cannot be empty")
    private String name;

    @Size(max = 200, message = "Film description cannot exceed 200 characters")
    private String description;

    private LocalDate releaseDate;

    private List<Genre> genres = new ArrayList<>();

    private MPA mpa;

    @Positive(message = "Film duration must be positive")
    private int duration;
    private Set<User> likes = new HashSet<>();
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private final LocalDate minReleaseDate = LocalDate.of(1895, Month.DECEMBER, 28);
}