package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.services.FilmService;

import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film addFilm(@Valid @RequestBody Film film) {
        return filmService.create(film);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<Film> getFilms() {
        return filmService.getFilms();
    }

    @GetMapping("/{filmId}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<Film> getFilmById(@PathVariable int filmId) {
        return filmService.getFilmById(filmId);
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public Collection<Film> getTopFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.getTopFilms(count);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Film updateFilms(@Valid @RequestBody Film film) {
        return filmService.update(film);
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Film addLikeFilm(@PathVariable int id, @PathVariable int userId) {
        return filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Film deleteLikeFilm(@PathVariable int id, @PathVariable int userId) {
        return filmService.deleteLike(id, userId);
    }
}