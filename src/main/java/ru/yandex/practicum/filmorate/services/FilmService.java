package ru.yandex.practicum.filmorate.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.ContentNotException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmDbStorage filmDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final MpaDbStorage mpaDbStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("filmDbStorage") FilmDbStorage filmDbStorage,
                       GenreDbStorage genreDbStorage,
                       MpaDbStorage mpaDbStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmDbStorage = filmDbStorage;
        this.genreDbStorage = genreDbStorage;
        this.mpaDbStorage = mpaDbStorage;
    }

    public Film create(Film film) {
        validate(film);
        return filmStorage.addFilm(film);
    }

    public Collection<Film> getFilms() {
        return filmStorage.getFilms();
    }

    public Optional<Film> getFilmById(int filmId) {
        return filmStorage.getFilmById(filmId);
    }

    public Film update(Film film) {
        validate(film);
        filmStorage.getFilmById(film.getId())
                .orElseThrow(() -> new NotFoundException("Film not found"));

        if (film.getMpa() != null) {
            mpaDbStorage.getMpaById(film.getMpa().getId())
                    .orElseThrow(() -> new NotFoundException("MPA not found"));
        }
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> uniqueGenreIds = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            List<Genre> uniqueGenres = uniqueGenreIds.stream()
                    .map(id -> genreDbStorage.getGenreById(id)
                            .orElseThrow(() -> new NotFoundException("Genre not found")))
                    .sorted(Comparator.comparing(Genre::getId))
                    .toList();
            film.setGenres(uniqueGenres);
        }

        return filmStorage.updateFilm(film);
    }

    public Film addLike(int filmId, int userId) {
        Film film = filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Film not found"));
        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (film.getLikes().contains(user)) {
            throw new ValidationException("User " + user.getId() + " has already liked this film");
        }

        filmDbStorage.addLike(filmId, userId);
        film.getLikes().add(user);
        return film;
    }

    public Film deleteLike(int filmId, int userId) {
        Film film = filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Film not found"));
        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!film.getLikes().contains(user)) {
            throw new ContentNotException("User " + user.getId() + " has not yet liked this film");
        }

        filmDbStorage.removeLike(filmId, userId);
        film.getLikes().remove(user);
        return film;
    }

    public List<Film> getTopFilms(int count) {
        if (count <= 0) {
            throw new ValidationException("The number of films must be positive");
        }
        return filmDbStorage.getTopFilms(count);
    }

    public Collection<Genre> getAllGenres() {
        return genreDbStorage.getAllGenres();
    }

    public Genre getGenreById(int id) {
        return genreDbStorage.getGenreById(id)
                .orElseThrow(() -> new NotFoundException("Genre not found"));
    }

    public Collection<MPA> getAllMpa() {
        return mpaDbStorage.getAllMpa();
    }

    public MPA getMpaById(int id) {
        return mpaDbStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("MPA not found"));
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Film name cannot be empty");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Film description cannot exceed 200 characters");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(film.getMinReleaseDate())) {
            throw new ValidationException("Release date cannot be earlier than " + film.getMinReleaseDate());
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Film duration must be positive");
        }
    }
}