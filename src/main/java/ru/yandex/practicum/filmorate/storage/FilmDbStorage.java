package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@Primary
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreDbStorage genreDbStorage;
    private final MpaDbStorage mpaDbStorage;

    public FilmDbStorage(JdbcTemplate jdbcTemplate, GenreDbStorage genreDbStorage, MpaDbStorage mpaDbStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreDbStorage = genreDbStorage;
        this.mpaDbStorage = mpaDbStorage;
    }

    @Override
    public Film addFilm(Film film) {

        if (film.getMpa() != null || film.getMpa().getName() != null) {
            int mpaId = film.getMpa().getId();
            String checkMpaSql = "SELECT COUNT(*) FROM MPA WHERE id = ?";
            Integer mpaCount = jdbcTemplate.queryForObject(checkMpaSql, Integer.class, mpaId);
            if (mpaCount == 0) {
                throw new NotFoundException("MPA not found");
            }
        } else {
            throw new ValidationException("MPA cannot be null");
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {

            Set<Genre> uniqueGenres = new HashSet<>(film.getGenres());
            film.setGenres(new ArrayList<>(uniqueGenres));

            List<Integer> genreIds = film.getGenres().stream().map(Genre::getId).toList();

            String checkGenresSql = "SELECT id FROM Genres WHERE id IN (:genreIds)";
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("genreIds", genreIds);

            NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
            List<Integer> existingGenreIds = namedParameterJdbcTemplate.queryForList(checkGenresSql, parameters, Integer.class);

            if (existingGenreIds.size() != genreIds.size()) {
                Set<Integer> missingGenres = new HashSet<>(genreIds);
                existingGenreIds.forEach(missingGenres::remove);
                throw new NotFoundException(missingGenres.toString());
            }
        }

        String sql = "INSERT INTO Films (name, description, releaseDate, mpa_id, duration) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null);
            stmt.setObject(4, film.getMpa() != null ? film.getMpa().getId() : null);
            stmt.setInt(5, film.getDuration());
            return stmt;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String genreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(genreSql, film.getId(), genre.getId());
            }
        }

        log.info("Film created with id: {}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE Films SET name = ?, description = ?, releaseDate = ?, mpa_id = ?, duration = ? WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(sql, film.getName(), film.getDescription(), film.getReleaseDate(), film.getMpa() != null ? film.getMpa().getId() : null, film.getDuration(), film.getId());
        if (rowsUpdated == 0) {
            throw new NotFoundException("Film not found");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String genreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(genreSql, film.getId(), genre.getId());
            }
        }

        log.info("Film updated with id: {}", film.getId());
        return film;
    }

    @Override
    public Collection<Film> getFilms() {
        String sql = "SELECT f.*, " +
                "u.id AS like_user_id, u.login AS like_user_login, u.email AS like_user_email, " +
                "u.name AS like_user_name, u.birthday AS like_user_birthday " +
                "FROM Films f " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "LEFT JOIN Users u ON fl.like_user_id = u.id " +
                "ORDER BY f.id";
        return jdbcTemplate.query(sql, this::mapRowToFilm).stream()
                .toList();
    }

    @Override
    public Optional<Film> getFilmById(int filmId) {
        String sql = "SELECT f.*, " +
                "m.id AS mpa_id, m.name AS mpa_name, " +
                "g.id AS genre_id, g.name AS genre_name, " +
                "u.id AS like_user_id, u.login AS like_user_login, " +
                "u.email AS like_user_email, u.name AS like_user_name, " +
                "u.birthday AS like_user_birthday " +
                "FROM Films f " +
                "LEFT JOIN MPA m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN Genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "LEFT JOIN Users u ON fl.like_user_id = u.id " +
                "WHERE f.id = ?";
        Optional<Film> film = jdbcTemplate.query(sql, this::mapRowToFilm, filmId)
                .stream()
                .findFirst();
        film.ifPresent(value -> value.setGenres(genreDbStorage.getGenresByFilmId(value.getId())));
        return film;
    }

    @Override
    public List<Film> getTopFilms(int count) {
        String sql = "SELECT f.*, COUNT(fl.like_user_id) as like_count " +
                "FROM Films f " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id " +
                "ORDER BY like_count DESC, f.id ASC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        for (Film film : films) {
            film.setGenres(genreDbStorage.getGenresByFilmId(film.getId()));
            String sql2 = "SELECT u.* FROM Users u " +
                    "JOIN film_likes fl ON u.id = fl.like_user_id " +
                    "WHERE fl.film_id = ?";
            film.setLikes(new HashSet<>(jdbcTemplate.query(sql2, this::mapRowToUser, film.getId())));
        }
        return films;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        LocalDate releaseDate = rs.getDate("releaseDate") != null ? rs.getDate("releaseDate").toLocalDate() : null;
        film.setReleaseDate(releaseDate);
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            film.setMpa(mpaDbStorage.getMpaById(mpaId).orElse(null));
        }

        return film;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    public void addLike(int filmId, int userId) {
        String sql = "INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND like_user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

}