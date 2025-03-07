package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate FILM_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final Map<Integer, Film> films = new HashMap<>();
    private int currentId = 0;

    @GetMapping
    public Collection<Film> getAllFilms() {
        log.info("Retrieving all films. Total count: {}", films.size());
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        validateFilm(film);

        film.setId(++currentId);
        films.put(film.getId(), film);
        log.info("Film created successfully. ID: {}", film.getId());
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film updatedFilm) {
        validateFilm(updatedFilm);
        if (!films.containsKey(updatedFilm.getId())) {
            log.error("Attempt to update non-existent film. ID: {}", updatedFilm.getId());
            throw new ValidationException("Film with ID " + updatedFilm.getId() + " not found.");
        }

        films.put(updatedFilm.getId(), updatedFilm);
        log.info("Film updated successfully. ID: {}", updatedFilm.getId());
        return updatedFilm;
    }

    private void validateFilm(Film film) {
        if (!StringUtils.hasText(film.getName())) {
            log.error("Film name validation failed. Name is blank.");
            throw new ValidationException("Film name cannot be blank.");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Film description validation failed. Description length: {}", film.getDescription() == null ? "null" : film.getDescription().length());
            throw new ValidationException("Film description cannot be null or exceed 200 characters.");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FILM_BIRTHDAY)) {
            log.error("Film release date validation failed. Date: {}", film.getReleaseDate());
            throw new ValidationException("Film release date cannot be null or before " + FILM_BIRTHDAY);
        }

        if (film.getDuration() <= 0) {
            log.error("Film duration validation failed. Duration: {}", film.getDuration());
            throw new ValidationException("Film duration must be positive.");
        }
    }
}