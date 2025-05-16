package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.services.FilmService;

import java.util.Collection;

@RestController
@RequestMapping("/genres")
@Slf4j
public class GenreController {

    private final FilmService filmService;

    @Autowired
    public GenreController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<Genre> getAllGenres() {
        return filmService.getAllGenres();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Genre getGenreById(@PathVariable int id) {
        return filmService.getGenreById(id);
    }
}