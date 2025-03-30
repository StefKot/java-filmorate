package ru.yandex.practicum.filmorate.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public void addLike(int filmId, int userId) {
        getFilmById(filmId);
        userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("User with ID " + userId + " not found"));
        Film film = getFilmById(filmId);
        film.addLike(userId);
        filmStorage.update(film);
        log.info("User {} liked film {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        getFilmById(filmId);
        userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("User with ID " + userId + " not found"));

        Film film = getFilmById(filmId);
        film.removeLike(userId);
        filmStorage.update(film);
        log.info("User {} removed like from film {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            log.error("Invalid count value: {}", count);
            throw new ValidationException("Count must be greater than 0");
        }
        log.info("Getting {} popular films", count);
        return filmStorage.getAllFilms().stream().sorted(Comparator.<Film>comparingInt(film -> film.getLikeScore().size()).reversed()).limit(count).collect(Collectors.toList());
    }

    public Collection<Film> getAllFilms() {
        log.info("Getting all films");
        return filmStorage.getAllFilms();
    }

    public Film create(Film film) {
        validateFilm(film);
        Film createdFilm = filmStorage.create(film);
        log.info("Created film with ID: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film update(Film film) {
        validateFilm(film);
        getFilmById(film.getId());
        Film updatedFilm = filmStorage.update(film);
        log.info("Updated film with ID: {}", updatedFilm.getId());
        return updatedFilm;
    }

    private Film getFilmById(int filmId) {
        return filmStorage.getFilmById(filmId).orElseThrow(() -> {
            log.error("Film with ID {} not found", filmId);
            return new NotFoundException("Film with ID " + filmId + " not found");
        });
    }

    private void validateFilm(Film film) {
        if (film == null) {
            log.error("Provided null film");
            throw new ValidationException("Film cannot be null");
        }

        if (film.getName() == null || film.getName().isEmpty()) {
            log.error("Empty film name");
            throw new ValidationException("Film name cannot be empty");
        }

        if (film.getDescription() == null || film.getDescription().length() > 200) {
            log.error("Invalid film description");
            throw new ValidationException("Film description length should be up to 200 characters");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.error("Invalid film release date");
            throw new ValidationException("Film release date cannot be before December 28, 1895");
        }
        if (film.getDuration() <= 0) {
            log.error("Invalid film duration");
            throw new ValidationException("Film duration must be positive");
        }
    }
}