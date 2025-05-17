package ru.yandex.practicum.filmorate.storage.interfaces;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    Collection<Genre> getAllGenres();

    Optional<Genre> getGenreById(int id);

    List<Genre> getGenresByFilmId(int filmId);

    List<Genre> getGenresByIds(Collection<Integer> ids);
}