package ru.yandex.practicum.filmorate.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.interfaces.GenreStorage;

import java.util.Collection;

@Slf4j
@Service
public class GenreService {

    private final GenreStorage genreStorage;

    @Autowired
    public GenreService(GenreStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    public Collection<Genre> getAllGenres() {
        log.info("Received request to get all genres");
        Collection<Genre> genres = genreStorage.getAllGenres();
        log.info("Returning {} genres", genres.size());
        return genres;
    }

    public Genre getGenreById(int id) {
        log.info("Received request to get genre by ID: {}", id);
        Genre genre = genreStorage.getGenreById(id)
                .orElseThrow(() -> {
                    log.error("Genre with ID {} not found", id);
                    return new NotFoundException("Genre not found");
                });
        log.info("Returning genre with ID: {}", id);
        return genre;
    }
}