package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.interfaces.GenreStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Collection<Genre> getAllGenres() {
        log.debug("Attempting to retrieve all genres from database");
        String sql = "SELECT * FROM Genres";
        List<Genre> genres = jdbcTemplate.query(sql, this::mapRowToGenre);
        log.debug("Retrieved {} genres from database", genres.size());
        return genres.stream().sorted(Comparator.comparing(Genre::getId)).toList();
    }

    @Override
    public Optional<Genre> getGenreById(int id) {
        log.debug("Attempting to retrieve genre by ID {} from database", id);
        String sql = "SELECT * FROM Genres WHERE id = ?";
        Optional<Genre> genre = jdbcTemplate.query(sql, this::mapRowToGenre, id).stream().findFirst();
        if (genre.isPresent()) {
            log.debug("Genre with ID {} found", id);
        } else {
            log.debug("Genre with ID {} not found in database", id);
        }
        return genre;
    }

    @Override
    public List<Genre> getGenresByFilmId(int filmId) {
        log.debug("Attempting to retrieve genres for film ID {} from database", filmId);
        String sql = "SELECT g.* FROM Genres g " + "JOIN film_genres fg ON g.id = fg.genre_id " + "WHERE fg.film_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, this::mapRowToGenre, filmId);
        log.debug("Retrieved {} genres for film ID {}", genres.size(), filmId);
        return genres.stream().sorted(Comparator.comparing(Genre::getId)).toList();
    }

    @Override
    public List<Genre> getGenresByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        log.debug("Attempting to retrieve genres by IDs {} from database", ids);
        String sql = "SELECT * FROM Genres WHERE id IN (:ids)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);
        List<Genre> genres = namedParameterJdbcTemplate.query(sql, parameters, this::mapRowToGenre);

        log.debug("Retrieved {} genres for IDs {}", genres.size(), ids);
        return genres.stream()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}