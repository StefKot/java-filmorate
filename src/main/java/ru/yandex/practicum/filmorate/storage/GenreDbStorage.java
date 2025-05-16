package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<Genre> getAllGenres() {
        String sql = "SELECT * FROM Genres";
        List<Genre> genres = jdbcTemplate.query(sql, this::mapRowToGenre);
        return genres.stream().sorted(Comparator.comparing(Genre::getId)).toList();
    }

    public Optional<Genre> getGenreById(int id) {
        String sql = "SELECT * FROM Genres WHERE id = ?";
        return jdbcTemplate.query(sql, this::mapRowToGenre, id).stream().findFirst();
    }

    public List<Genre> getGenresByFilmId(int filmId) {
        String sql = "SELECT g.* FROM Genres g " + "JOIN film_genres fg ON g.id = fg.genre_id " + "WHERE fg.film_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, this::mapRowToGenre, filmId);
        return genres.stream().sorted(Comparator.comparing(Genre::getId)).toList();
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}