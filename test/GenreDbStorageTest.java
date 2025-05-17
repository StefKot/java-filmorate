package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Genre;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({GenreDbStorage.class})
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;
    private final JdbcTemplate jdbcTemplate;

    private int insertFilm(String name, LocalDate releaseDate, int duration, int mpaId) {
        String sql = "INSERT INTO Films (name, releaseDate, duration, mpa_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, name, releaseDate, duration, mpaId);
        Integer filmId = jdbcTemplate.queryForObject("SELECT id FROM Films WHERE name = ? ORDER BY id DESC LIMIT 1", Integer.class, name);
        return filmId != null ? filmId : -1;
    }

    private void insertFilmGenre(int filmId, int genreId) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, genreId);
    }

    @Test
    @DisplayName("Get all genres")
    void testGetAllGenres() {
        Collection<Genre> genres = genreStorage.getAllGenres();

        assertThat(genres).isNotNull();
        assertThat(genres).hasSize(6);
        assertThat(genres).extracting(Genre::getId).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(genres).extracting(Genre::getName).containsExactly(
                "Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    @DisplayName("Get genre by existing ID")
    void testGetGenreByIdExisting() {
        Optional<Genre> genreOptional = genreStorage.getGenreById(1);

        assertThat(genreOptional).isPresent();
        Genre genre = genreOptional.get();
        assertThat(genre.getId()).isEqualTo(1);
        assertThat(genre.getName()).isEqualTo("Комедия");
    }

    @Test
    @DisplayName("Get genre by non-existent ID")
    void testGetGenreByIdNonExistent() {
        Optional<Genre> genreOptional = genreStorage.getGenreById(999);

        assertThat(genreOptional).isEmpty();
    }

    @Test
    @DisplayName("Get genres by film ID with genres")
    void testGetGenresByFilmIdWithGenres() {
        int filmId = insertFilm("Film With Genres", LocalDate.of(2020, 1, 1), 90, 1); // Insert a film
        insertFilmGenre(filmId, 1);
        insertFilmGenre(filmId, 3);
        insertFilmGenre(filmId, 2);

        List<Genre> genres = genreStorage.getGenresByFilmId(filmId);

        assertThat(genres).isNotNull();
        assertThat(genres).hasSize(3);
        assertThat(genres).extracting(Genre::getId).containsExactly(1, 2, 3);
        assertThat(genres).extracting(Genre::getName).containsExactly("Комедия", "Драма", "Мультфильм");
    }

    @Test
    @DisplayName("Get genres by film ID with no genres")
    void testGetGenresByFilmIdWithNoGenres() {
        int filmId = insertFilm("Film Without Genres", LocalDate.of(2021, 1, 1), 100, 1);

        List<Genre> genres = genreStorage.getGenresByFilmId(filmId);

        assertThat(genres).isNotNull();
        assertThat(genres).isEmpty();
    }

    @Test
    @DisplayName("Get genres by non-existent film ID")
    void testGetGenresByFilmIdNonExistentFilm() {
        List<Genre> genres = genreStorage.getGenresByFilmId(999);

        assertThat(genres).isNotNull();
        assertThat(genres).isEmpty();
    }
}