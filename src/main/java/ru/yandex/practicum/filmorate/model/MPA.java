package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MPA {
    private int id;
    @NotBlank(message = "MPA rating name cannot be empty")
    private String name;

    public MPA(int mpaId, String mpaName) {
        this.id = mpaId;
        this.name = mpaName;
    }
}