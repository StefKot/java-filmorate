package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
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
        log.debug("Attempting to add film: {}", film.getName());

        validateFilmData(film);

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

        int filmId = keyHolder.getKey().intValue();
        film.setId(filmId);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String genreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = film.getGenres().stream()
                    .map(genre -> new Object[]{filmId, genre.getId()})
                    .toList();
            jdbcTemplate.batchUpdate(genreSql, batchArgs);
        }

        log.info("FilmDbStorage: Film created with id: {}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        log.debug("Attempting to update film with ID: {}", film.getId());

        String checkFilmSql = "SELECT COUNT(*) FROM Films WHERE id = ?";
        Integer filmCount = jdbcTemplate.queryForObject(checkFilmSql, Integer.class, film.getId());
        if (filmCount == null || filmCount == 0) {
            log.error("Film with ID {} not found for update", film.getId());
            throw new NotFoundException("Film not found");
        }

        validateFilmData(film);


        String sql = "UPDATE Films SET name = ?, description = ?, releaseDate = ?, mpa_id = ?, duration = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getDuration(),
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String genreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = film.getGenres().stream()
                    .map(genre -> new Object[]{film.getId(), genre.getId()})
                    .toList();
            jdbcTemplate.batchUpdate(genreSql, batchArgs);
        }

        log.info("FilmDbStorage: Film updated with id: {}", film.getId());
        return film;
    }

    @Override
    public Collection<Film> getFilms() {
        log.debug("Attempting to retrieve all films with genres and likes from database");
        String sql = "SELECT f.id AS film_id, f.name AS film_name, f.description, f.releaseDate, f.duration, " +
                "m.id AS mpa_id, m.name AS mpa_name, " +
                "g.id AS genre_id, g.name AS genre_name, " +
                "fl.like_user_id " +
                "FROM Films f " +
                "JOIN MPA m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN Genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "ORDER BY f.id, g.id, fl.like_user_id";

        return jdbcTemplate.query(sql, rs -> {
            Map<Integer, Film> filmMap = new LinkedHashMap<>();

            while (rs.next()) {
                Integer filmId = rs.getInt("film_id");
                Film film = filmMap.computeIfAbsent(filmId, id -> {
                    Film newFilm = new Film();
                    try {
                        newFilm.setId(id);
                        newFilm.setName(rs.getString("film_name"));
                        newFilm.setDescription(rs.getString("description"));
                        Date releaseDateSql = rs.getDate("releaseDate");
                        newFilm.setReleaseDate(releaseDateSql != null ? releaseDateSql.toLocalDate() : null);
                        newFilm.setDuration(rs.getInt("duration"));

                        MPA mpa = new MPA();
                        mpa.setId(rs.getInt("mpa_id"));
                        mpa.setName(rs.getString("mpa_name"));
                        newFilm.setMpa(mpa);

                        newFilm.setGenres(new ArrayList<>());
                        newFilm.setLikes(new HashSet<>());
                    } catch (SQLException e) {
                        log.error("Error mapping film data from ResultSet", e);
                        throw new RuntimeException("Error mapping film data", e);
                    }
                    return newFilm;
                });

                Integer genreId = rs.getInt("genre_id");
                if (!rs.wasNull()) {
                    Genre genre = new Genre();
                    try {
                        genre.setId(genreId);
                        genre.setName(rs.getString("genre_name"));
                    } catch (SQLException e) {
                        log.error("Error mapping genre data from ResultSet for film ID {}", filmId, e);
                        throw new RuntimeException("Error mapping genre data", e);
                    }
                    if (!film.getGenres().stream().anyMatch(g -> g.getId() == genre.getId())) {
                        film.getGenres().add(genre);
                    }
                }

                Integer likeUserId = rs.getInt("like_user_id");
                if (!rs.wasNull()) {
                    User likeUser = new User();
                    likeUser.setId(likeUserId);
                    film.getLikes().add(likeUser);
                }
            }

            log.debug("Finished processing ResultSet for getFilms. Total films found: {}", filmMap.size());

            filmMap.values().forEach(film -> {
                if (film.getGenres() != null) {
                    List<Genre> distinctGenres = new ArrayList<>(new HashSet<>(film.getGenres()));
                    distinctGenres.sort(Comparator.comparing(Genre::getId));
                    film.setGenres(distinctGenres);
                }
            });

            return filmMap.values();
        });
    }

    @Override
    public Optional<Film> getFilmById(int filmId) {
        log.debug("Attempting to retrieve film by ID {} with genres and likes from database", filmId);
        String sql = "SELECT f.id AS film_id, f.name AS film_name, f.description, f.releaseDate, f.duration, " +
                "m.id AS mpa_id, m.name AS mpa_name, " +
                "g.id AS genre_id, g.name AS genre_name, " +
                "fl.like_user_id " +
                "FROM Films f " +
                "JOIN MPA m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN Genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "WHERE f.id = ? " +
                "ORDER BY g.id, fl.like_user_id";

        Film film = jdbcTemplate.query(sql, rs -> {
            Film currentFilm = null;
            while (rs.next()) {
                if (currentFilm == null) {
                    currentFilm = new Film();
                    try {
                        currentFilm.setId(rs.getInt("film_id"));
                        currentFilm.setName(rs.getString("film_name"));
                        currentFilm.setDescription(rs.getString("description"));
                        Date releaseDateSql = rs.getDate("releaseDate");
                        currentFilm.setReleaseDate(releaseDateSql != null ? releaseDateSql.toLocalDate() : null);
                        currentFilm.setDuration(rs.getInt("duration"));

                        MPA mpa = new MPA();
                        mpa.setId(rs.getInt("mpa_id"));
                        mpa.setName(rs.getString("mpa_name"));
                        currentFilm.setMpa(mpa);

                        currentFilm.setGenres(new ArrayList<>());
                        currentFilm.setLikes(new HashSet<>());
                    } catch (SQLException e) {
                        log.error("Error mapping film data from ResultSet for ID {}", filmId, e);
                        throw new RuntimeException("Error mapping film data", e);
                    }
                }

                Integer genreId = rs.getInt("genre_id");
                if (!rs.wasNull()) {
                    Genre genre = new Genre();
                    try {
                        genre.setId(genreId);
                        genre.setName(rs.getString("genre_name"));
                    } catch (SQLException e) {
                        log.error("Error mapping genre data from ResultSet for film ID {}", filmId, e);
                        throw new RuntimeException("Error mapping genre data", e);
                    }
                    if (!currentFilm.getGenres().stream().anyMatch(g -> g.getId() == genre.getId())) {
                        currentFilm.getGenres().add(genre);
                    }
                }

                Integer likeUserId = rs.getInt("like_user_id");
                if (!rs.wasNull()) {
                    User likeUser = new User();
                    likeUser.setId(likeUserId);
                    currentFilm.getLikes().add(likeUser);
                }
            }

            if (currentFilm != null && currentFilm.getGenres() != null) {
                currentFilm.getGenres().sort(Comparator.comparing(Genre::getId));
            }
            return currentFilm;
        }, filmId);

        Optional<Film> resultOptional = Optional.ofNullable(film);
        log.debug("Film with ID {} found: {}", filmId, resultOptional.isPresent());

        return resultOptional;
    }

    @Override
    public List<Film> getTopFilms(int count) {
        log.debug("Attempting to retrieve top {} films with genres and likes from database", count);
        String sql = "SELECT f.id AS film_id, f.name AS film_name, f.description, f.releaseDate, f.duration, " +
                "m.id AS mpa_id, m.name AS mpa_name, " +
                "g.id AS genre_id, g.name AS genre_name, " +
                "fl.like_user_id, " +
                "u.id AS like_user_id_full, u.login AS like_user_login, u.email AS like_user_email, u.name AS like_user_name, u.birthday AS like_user_birthday " +
                "FROM Films f " +
                "JOIN MPA m ON f.mpa_id = m.id " +
                "LEFT JOIN film_genres fg ON f.id = fg.film_id " +
                "LEFT JOIN Genres g ON fg.genre_id = g.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "LEFT JOIN Users u ON fl.like_user_id = u.id " +
                "WHERE f.id IN ( " +
                "  SELECT f_top.id " +
                "  FROM Films f_top " +
                "  LEFT JOIN film_likes fl_top ON f_top.id = fl_top.film_id " +
                "  GROUP BY f_top.id " +
                "  ORDER BY COUNT(fl_top.like_user_id) DESC, f_top.id ASC " +
                "  LIMIT ? " +
                ") " +
                "ORDER BY ( " +
                "  SELECT COUNT(fl_order.like_user_id) " +
                "  FROM film_likes fl_order " +
                "  WHERE fl_order.film_id = f.id " +
                ") DESC NULLS LAST, f.id ASC, g.id ASC, fl.like_user_id ASC";

        List<Film> films = jdbcTemplate.query(sql, rs -> {
            Map<Integer, Film> currentFilmMap = new LinkedHashMap<>();

            while (rs.next()) {
                Integer filmId = rs.getInt("film_id");
                Film film = currentFilmMap.computeIfAbsent(filmId, id -> {
                    Film newFilm = new Film();
                    try {
                        newFilm.setId(id);
                        newFilm.setName(rs.getString("film_name"));
                        newFilm.setDescription(rs.getString("description"));
                        Date releaseDateSql = rs.getDate("releaseDate");
                        newFilm.setReleaseDate(releaseDateSql != null ? releaseDateSql.toLocalDate() : null);
                        newFilm.setDuration(rs.getInt("duration"));

                        MPA mpa = new MPA();
                        mpa.setId(rs.getInt("mpa_id"));
                        mpa.setName(rs.getString("mpa_name"));
                        newFilm.setMpa(mpa);

                        newFilm.setGenres(new ArrayList<>());
                        newFilm.setLikes(new HashSet<>());

                    } catch (SQLException e) {
                        log.error("Error mapping film data from ResultSet for top films", e);
                        throw new RuntimeException("Error mapping film data", e);
                    }
                    return newFilm;
                });

                Integer genreId = rs.getInt("genre_id");
                if (!rs.wasNull()) {
                    Genre genre = new Genre();
                    try {
                        genre.setId(genreId);
                        genre.setName(rs.getString("genre_name"));
                    } catch (SQLException e) {
                        log.error("Error mapping genre data from ResultSet for top film ID {}", filmId, e);
                        throw new RuntimeException("Error mapping genre data", e);
                    }
                    if (!film.getGenres().stream().anyMatch(g -> g.getId() == genre.getId())) {
                        film.getGenres().add(genre);
                    }
                }

                Integer likeUserId = rs.getInt("like_user_id");
                if (!rs.wasNull()) {
                    User likeUser = new User();
                    try {
                        likeUser.setId(rs.getInt("like_user_id_full"));
                        likeUser.setLogin(rs.getString("like_user_login"));
                        likeUser.setEmail(rs.getString("like_user_email"));
                        likeUser.setName(rs.getString("like_user_name"));
                        Date birthdaySql = rs.getDate("like_user_birthday");
                        likeUser.setBirthday(birthdaySql != null ? birthdaySql.toLocalDate() : null);
                    } catch (SQLException e) {
                        log.error("Error mapping user data from ResultSet for top film ID {}", filmId, e);
                        throw new RuntimeException("Error mapping user data", e);
                    }
                    film.getLikes().add(likeUser);
                }
            }

            log.debug("Finished processing ResultSet for getTopFilms. Total films found: {}", currentFilmMap.size());

            currentFilmMap.values().forEach(film -> {
                if (film.getGenres() != null) {
                    List<Genre> distinctGenres = new ArrayList<>(new HashSet<>(film.getGenres()));
                    distinctGenres.sort(Comparator.comparing(Genre::getId));
                    film.setGenres(distinctGenres);
                }
            });

            return new ArrayList<>(currentFilmMap.values());
        }, count);

        return films;
    }


    public void addLike(int filmId, int userId) {
        log.debug("FilmDbStorage: Attempting to add like for film {} by user {}", filmId, userId);
        String sql = "INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("FilmDbStorage: Like added for film {} by user {}", filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("FilmDbStorage: Attempting to remove like for film {} by user {}", filmId, userId);
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND like_user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("FilmDbStorage: Like removed for film {} by user {}", filmId, userId);
    }

    public boolean checkLikeExists(int filmId, int userId) {
        log.debug("FilmDbStorage: Checking if like exists for film {} by user {}", filmId, userId);
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND like_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        boolean exists = count != null && count > 0;
        log.debug("FilmDbStorage: Like exists for film {} by user {}: {}", filmId, userId, exists);
        return exists;
    }

    private void validateFilmData(Film film) {
        log.debug("FilmDbStorage: Validating film data for film: {}", film != null ? film.getName() : "null");

        if (film == null) {
            log.error("Validation failed: Film object is null");
            throw new ValidationException("Film cannot be null");
        }

        if (film.getMpa() == null) {
            log.error("Validation failed: MPA cannot be null for film {}", film.getName());
            throw new ValidationException("MPA cannot be null");
        }
        int mpaId = film.getMpa().getId();
        String checkMpaSql = "SELECT COUNT(*) FROM MPA WHERE id = ?";
        Integer mpaCount = jdbcTemplate.queryForObject(checkMpaSql, Integer.class, mpaId);
        if (mpaCount == null || mpaCount == 0) {
            log.error("Validation failed: MPA with ID {} not found for film {}", mpaId, film.getName());
            throw new NotFoundException("MPA not found");
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
                log.error("Validation failed: Genres with IDs {} not found for film {}", missingGenres, film.getName());
                throw new NotFoundException("Genres not found: " + missingGenres);
            }
            film.getGenres().sort(Comparator.comparing(Genre::getId));
        }
        log.debug("FilmDbStorage: Film data validation successful");
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
}