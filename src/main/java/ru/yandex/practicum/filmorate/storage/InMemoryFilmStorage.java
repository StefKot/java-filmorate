package ru.yandex.practicum.filmorate.storage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private static final LocalDate FIRST_FILM_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    @Getter
    private final Map<Integer, Film> films = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public Film create(Film film) {
        int id = idGenerator.incrementAndGet();
        film.setId(id);
        films.put(id, film);
        log.info("Created film with ID: {}", film.getId());
        log.debug("Film: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        int filmId = film.getId();

        if (!films.containsKey(filmId)) {
            log.error("Film with ID {} not found", filmId);
            throw new NotFoundException("Film with ID " + filmId + " not found");
        }

        films.put(filmId, film);
        log.info("Updated film with ID: {}", film.getId());
        log.debug("Film: {}", film);
        return film;
    }

    @Override
    public void delete(int filmId) {
        if (!films.containsKey(filmId)) {
            log.error("Film with ID {} not found for deletion", filmId);
            throw new NotFoundException("Film with ID " + filmId + " not found");
        }
        films.remove(filmId);
        log.info("Film with ID {} deleted", filmId);
    }

    @Override
    public Optional<Film> getFilmById(int id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }
}