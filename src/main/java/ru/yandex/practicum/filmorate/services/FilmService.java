package ru.yandex.practicum.filmorate.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exceptions.ContentNotException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.GenreStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.MPAStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.util.*;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmDbStorage filmDbStorage;
    private final GenreStorage genreStorage;
    private final MPAStorage mpaStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("filmDbStorage") FilmDbStorage filmDbStorage,
                       GenreStorage genreStorage,
                       MPAStorage mpaStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmDbStorage = filmDbStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    @Transactional
    public Film create(Film film) {
        log.info("FilmService: received request to create film {}", film.getName());
        validate(film);
        Film createdFilm = filmStorage.addFilm(film);

        List<Genre> genres = genreStorage.getGenresByFilmId(createdFilm.getId());
        createdFilm.setGenres(genres);
        if (createdFilm.getMpa() != null && (createdFilm.getMpa().getName() == null || createdFilm.getMpa().getName().isEmpty())) {
            mpaStorage.getMpaById(createdFilm.getMpa().getId()).ifPresent(createdFilm::setMpa);
        }

        log.info("FilmService: film created with ID: {}", createdFilm.getId());
        return createdFilm;
    }

    public Collection<Film> getFilms() {
        log.info("FilmService: received request to get all films");
        Collection<Film> films = filmStorage.getFilms();
        log.info("FilmService: retrieved {} films from storage (enriched)", films.size());
        return films;
    }

    public Optional<Film> getFilmById(int filmId) {
        log.info("FilmService: received request to get film by ID: {}", filmId);
        Optional<Film> filmOptional = filmStorage.getFilmById(filmId);

        if (filmOptional.isEmpty()) {
            log.warn("FilmService: film with ID {} not found", filmId);
        } else {
            log.info("FilmService: returning film with ID: {}", filmId);
        }

        return filmOptional;
    }

    @Transactional
    public Film update(Film film) {
        log.info("FilmService: received request to update film with ID: {}", film.getId());
        validate(film);
        findFilmById(film.getId());

        Film updatedFilm = filmStorage.updateFilm(film);

        List<Genre> updatedGenres = genreStorage.getGenresByFilmId(updatedFilm.getId());
        updatedFilm.setGenres(updatedGenres);
        if (updatedFilm.getMpa() != null && (updatedFilm.getMpa().getName() == null || updatedFilm.getMpa().getName().isEmpty())) {
            mpaStorage.getMpaById(updatedFilm.getMpa().getId()).ifPresent(updatedFilm::setMpa);
        }
        log.debug("FilmService: Re-fetched {} genres for updated film ID {}", updatedGenres.size(), updatedFilm.getId());

        log.info("FilmService: Film with ID {} updated successfully", updatedFilm.getId());
        return updatedFilm;
    }

    @Transactional
    public Film addLike(int filmId, int userId) {
        log.info("FilmService: received request to add like to film ID {} by user ID {}", filmId, userId);
        Film film = findFilmById(filmId);
        User user = findUserById(userId);

        List<Genre> genres = genreStorage.getGenresByFilmId(film.getId());
        film.setGenres(genres);
        if (film.getMpa() != null && (film.getMpa().getName() == null || film.getMpa().getName().isEmpty())) {
            mpaStorage.getMpaById(film.getMpa().getId()).ifPresent(film::setMpa);
        }
        log.debug("FilmService: Enriched film ID {} before adding like", film.getId());

        if (filmDbStorage.checkLikeExists(filmId, userId)) {
            log.warn("FilmService: User {} already liked film {}", userId, filmId);
            throw new ValidationException("User " + userId + " has already liked this film");
        }

        filmDbStorage.addLike(filmId, userId);
        film.getLikes().add(user);

        log.info("FilmService: User {} liked film {}", userId, filmId);
        return film;
    }

    @Transactional
    public Film deleteLike(int filmId, int userId) {
        log.info("FilmService: received request to delete like from film ID {} by user ID {}", filmId, userId);
        Film film = findFilmById(filmId);
        User user = findUserById(userId);

        List<Genre> genres = genreStorage.getGenresByFilmId(film.getId());
        film.setGenres(genres);
        if (film.getMpa() != null && (film.getMpa().getName() == null || film.getMpa().getName().isEmpty())) {
            mpaStorage.getMpaById(film.getMpa().getId()).ifPresent(film::setMpa);
        }
        log.debug("FilmService: Enriched film ID {} before deleting like", film.getId());

        boolean likeExistsInDb = filmDbStorage.checkLikeExists(filmId, userId);
        if (!likeExistsInDb) {
            log.warn("FilmService: User {} has no like on film {}", userId, filmId);
            throw new ContentNotException("User " + userId + " has not yet liked this film");
        }

        filmDbStorage.removeLike(filmId, userId);
        film.getLikes().remove(user);

        log.info("FilmService: User {} removed like from film {}", userId, filmId);
        return film;
    }


    public List<Film> getTopFilms(int count) {
        log.info("FilmService: received request to get top {} films", count);
        if (count <= 0) {
            log.error("FilmService: Invalid count {} for getting top films", count);
            throw new ValidationException("The number of films must be positive");
        }
        List<Film> topFilms = filmDbStorage.getTopFilms(count);
        log.info("FilmService: retrieved {} top films from storage (enriched)", topFilms.size());

        for (Film film : topFilms) {
            List<Genre> genres = genreStorage.getGenresByFilmId(film.getId());
            film.setGenres(genres);
            if (film.getMpa() != null && (film.getMpa().getName() == null || film.getMpa().getName().isEmpty())) {
                mpaStorage.getMpaById(film.getMpa().getId()).ifPresent(film::setMpa);
            }
            log.debug("FilmService: Enriched top film ID {} with {} genres", film.getId(), genres.size());
        }

        log.info("FilmService: returning {} top films enriched with data", topFilms.size());
        return topFilms;
    }


    private void validate(Film film) {
        log.debug("FilmService: Validating film: {}", film);
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("FilmService: Validation failed: Film name is empty or blank");
            throw new ValidationException("Film name cannot be empty");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("FilmService: Validation failed: Film description exceeds 200 characters");
            throw new ValidationException("Film description cannot exceed 200 characters");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(Film.MIN_RELEASE_DATE)) {
            log.error("FilmService: Validation failed: Release date {} is before {}", film.getReleaseDate(), Film.MIN_RELEASE_DATE);
            throw new ValidationException("Release date cannot be earlier than " + Film.MIN_RELEASE_DATE);
        }
        if (film.getDuration() <= 0) {
            log.error("FilmService: Validation failed: Film duration is not positive ({})", film.getDuration());
            throw new ValidationException("Film duration must be positive");
        }

        if (film.getMpa() != null) {
            mpaStorage.getMpaById(film.getMpa().getId())
                    .orElseThrow(() -> {
                        log.error("FilmService: Validation failed: Invalid MPA ID: {}", film.getMpa().getId());
                        return new ValidationException("Invalid MPA ID: " + film.getMpa().getId());
                    });
        } else {
            log.error("FilmService: Validation failed: MPA object is null");
            throw new ValidationException("MPA cannot be null");
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new HashSet<>(film.getGenres());
            List<Genre> sortedUniqueGenres = new ArrayList<>(uniqueGenres);
            sortedUniqueGenres.sort(Comparator.comparing(Genre::getId));
            film.setGenres(sortedUniqueGenres);

            for (Genre genre : sortedUniqueGenres) {
                genreStorage.getGenreById(genre.getId())
                        .orElseThrow(() -> {
                            log.error("FilmService: Validation failed: Invalid Genre ID: {}", genre.getId());
                            return new ValidationException("Invalid Genre ID: " + genre.getId());
                        });
            }
        }

        log.debug("FilmService: Film validation successful");
    }

    private Film findFilmById(int filmId) {
        log.debug("FilmService: Looking for film with ID: {}", filmId);
        return filmStorage.getFilmById(filmId)
                .orElseThrow(() -> {
                    log.error("FilmService: Film with ID {} not found", filmId);
                    return new NotFoundException("Film with ID " + filmId + " not found");
                });
    }

    private User findUserById(int userId) {
        log.debug("UserService: Looking for user with ID: {}", userId);
        return userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("UserService: User with ID {} not found", userId);
                    return new NotFoundException("User with ID " + userId + " not found");
                });
    }
}