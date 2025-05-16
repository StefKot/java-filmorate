package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Genre {
    private int id;
    @NotBlank(message = "Genre name cannot be empty")
    private String name;

    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
    }
}