package ru.yandex.practicum.filmorate.storage.interfaces;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    Film updateFilm(Film film);

    Film addFilm(Film film);

    Collection<Film> getFilms();

    Optional<Film> getFilmById(int id);

    List<Film> getTopFilms(int count);
}
