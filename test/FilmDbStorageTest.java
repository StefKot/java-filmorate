package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;


    private Film createTestFilmWithFullObjects(String name, String description, LocalDate releaseDate, int duration, int mpaId, List<Integer> genreIds) {
        MPA mpa = mpaStorage.getMpaById(mpaId).orElseThrow(() -> new RuntimeException("MPA not found in test setup"));

        List<Genre> genres = new ArrayList<>();
        if (genreIds != null && !genreIds.isEmpty()) {
            genres = genreStorage.getGenresByIds(genreIds);
        }

        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        film.setGenres(genres);
        film.setLikes(new HashSet<>());
        return film;
    }

    private Film createTestFilmWithIds(String name, String description, LocalDate releaseDate, int duration, int mpaId, List<Integer> genreIds) {
        MPA mpa = new MPA();
        mpa.setId(mpaId);

        List<Genre> genres = new ArrayList<>();
        if (genreIds != null) {
            for (Integer genreId : genreIds) {
                Genre genre = new Genre();
                genre.setId(genreId);
                genres.add(genre);
            }
        }

        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        film.setGenres(genres);
        film.setLikes(new HashSet<>());
        return film;
    }


    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        user.setFriends(new HashSet<>());
        return user;
    }

    private int insertUser(User user) {
        String sql = "INSERT INTO Users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), user.getBirthday());
        return jdbcTemplate.queryForObject("SELECT id FROM Users WHERE email = ? ORDER BY id DESC LIMIT 1", Integer.class, user.getEmail());
    }


    private int insertFilm(Film film) {
        String sql = "INSERT INTO Films (name, description, releaseDate, mpa_id, duration) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getDuration());

        Integer filmId = jdbcTemplate.queryForObject("SELECT id FROM Films WHERE name = ? ORDER BY id DESC LIMIT 1", Integer.class, film.getName());

        if (filmId != null && film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> uniqueGenreIds = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());

            String genreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = uniqueGenreIds.stream()
                    .map(genreId -> new Object[]{filmId, genreId})
                    .toList();
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(genreSql, batchArgs);
            }
        }
        return filmId != null ? filmId : -1;
    }


    @Test
    @DisplayName("Add film without genres")
    void testAddFilmWithoutGenres() {
        Film newFilm = createTestFilmWithFullObjects("Фильм 1", "Описание 1", LocalDate.of(2000, 1, 1), 120, 1, null);

        Film addedFilm = filmStorage.addFilm(newFilm);

        assertThat(addedFilm).isNotNull();
        assertThat(addedFilm.getId()).isPositive();
        assertThat(addedFilm.getName()).isEqualTo("Фильм 1");
        assertThat(addedFilm.getMpa()).isNotNull();
        assertThat(addedFilm.getMpa().getId()).isEqualTo(1);
        assertThat(addedFilm.getMpa().getName()).isEqualTo("G");
        assertThat(addedFilm.getGenres()).isEmpty();
        assertThat(addedFilm.getLikes()).isEmpty();


        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Films WHERE id = ?", Integer.class, addedFilm.getId());
        assertThat(count).isEqualTo(1);

        Integer genreCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM film_genres WHERE film_id = ?", Integer.class, addedFilm.getId());
        assertThat(genreCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Add film with genres")
    void testAddFilmWithGenres() {
        Film newFilm = createTestFilmWithFullObjects("Фильм с жанрами", "Описание", LocalDate.of(2010, 5, 15), 90, 2, List.of(1, 2));

        Film addedFilm = filmStorage.addFilm(newFilm);

        assertThat(addedFilm).isNotNull();
        assertThat(addedFilm.getId()).isPositive();
        assertThat(addedFilm.getMpa()).isNotNull();
        assertThat(addedFilm.getMpa().getId()).isEqualTo(2);
        assertThat(addedFilm.getMpa().getName()).isEqualTo("PG");
        assertThat(addedFilm.getGenres()).hasSize(2);
        assertThat(addedFilm.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
        assertThat(addedFilm.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
        assertThat(addedFilm.getLikes()).isEmpty();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Films WHERE id = ?", Integer.class, addedFilm.getId());
        assertThat(count).isEqualTo(1);

        List<Integer> genreIdsInDb = jdbcTemplate.queryForList("SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id", Integer.class, addedFilm.getId());
        assertThat(genreIdsInDb).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Adding film with invalid MPA throws DataIntegrityViolationException")
    void testAddFilmWithInvalidMpaThrowsException() {
        Film newFilm = createTestFilmWithIds("Фильм с плохим MPA", "Описание", LocalDate.of(2020, 1, 1), 100, 999, null);

        assertThatThrownBy(() -> filmStorage.addFilm(newFilm))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Adding film with invalid genre throws DataIntegrityViolationException")
    void testAddFilmWithInvalidGenreThrowsException() {
        Film newFilm = createTestFilmWithIds("Фильм с плохим жанром", "Описание", LocalDate.of(2020, 1, 1), 100, 1, List.of(1, 999));

        assertThatThrownBy(() -> filmStorage.addFilm(newFilm))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Adding film with duplicate genres should save only unique ones")
    void testAddFilmWithDuplicateGenres() {
        Film newFilm = createTestFilmWithFullObjects("Фильм с дубликатами", "Описание", LocalDate.of(2010, 5, 15), 90, 2, List.of(1, 2, 1));

        Film addedFilm = filmStorage.addFilm(newFilm);

        assertThat(addedFilm).isNotNull();
        assertThat(addedFilm.getId()).isPositive();
        assertThat(addedFilm.getMpa()).isNotNull();
        assertThat(addedFilm.getMpa().getId()).isEqualTo(2);
        assertThat(addedFilm.getMpa().getName()).isEqualTo("PG");
        assertThat(addedFilm.getGenres()).hasSize(2);
        assertThat(addedFilm.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
        assertThat(addedFilm.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
        assertThat(addedFilm.getLikes()).isEmpty();


        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Films WHERE id = ?", Integer.class, addedFilm.getId());
        assertThat(count).isEqualTo(1);

        List<Integer> genreIdsInDb = jdbcTemplate.queryForList("SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id", Integer.class, addedFilm.getId());
        assertThat(genreIdsInDb).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Update film")
    void testUpdateFilm() {
        Film originalFilm = createTestFilmWithFullObjects("Оригинальный фильм", "Оригинальное описание", LocalDate.of(2005, 10, 20), 110, 1, List.of(1));
        int filmId = insertFilm(originalFilm);

        Film updatedFilm = createTestFilmWithFullObjects("Обновленный фильм", "Новое описание", LocalDate.of(2006, 11, 21), 115, 2, List.of(2, 3));
        updatedFilm.setId(filmId);

        Film resultFilm = filmStorage.updateFilm(updatedFilm);

        assertThat(resultFilm).isNotNull();
        assertThat(resultFilm.getId()).isEqualTo(filmId);
        assertThat(resultFilm.getName()).isEqualTo("Обновленный фильм");
        assertThat(resultFilm.getDescription()).isEqualTo("Новое описание");
        assertThat(resultFilm.getReleaseDate()).isEqualTo(LocalDate.of(2006, 11, 21));
        assertThat(resultFilm.getDuration()).isEqualTo(115);
        assertThat(resultFilm.getMpa()).isNotNull();
        assertThat(resultFilm.getMpa().getId()).isEqualTo(2);
        assertThat(resultFilm.getMpa().getName()).isEqualTo("PG");
        assertThat(resultFilm.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(resultFilm.getGenres()).extracting(Genre::getName).containsExactly("Драма", "Мультфильм");
        assertThat(resultFilm.getLikes()).isEmpty();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Films WHERE id = ?", Integer.class, filmId);
        assertThat(count).isEqualTo(1);

        Optional<Film> filmFromDbOptional = filmStorage.getFilmById(filmId);
        assertThat(filmFromDbOptional).isPresent();
        Film filmFromDb = filmFromDbOptional.get();

        assertThat(filmFromDb.getName()).isEqualTo("Обновленный фильм");
        assertThat(filmFromDb.getMpa().getId()).isEqualTo(2);
        assertThat(filmFromDb.getGenres()).hasSize(2);
        assertThat(filmFromDb.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(filmFromDb.getLikes()).isEmpty();


        List<Integer> genreIdsFromDb = jdbcTemplate.queryForList("SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id", Integer.class, filmId);
        assertThat(genreIdsFromDb).containsExactly(2, 3);
    }

    @Test
    @DisplayName("Update non-existent film throws NotFoundException")
    void testUpdateNonExistentFilmThrowsException() {
        Film nonExistentFilm = createTestFilmWithIds("Несуществующий", "Описание", LocalDate.now(), 100, 1, null);
        nonExistentFilm.setId(999);

        assertThatThrownBy(() -> filmStorage.updateFilm(nonExistentFilm))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Film not found");
    }

    @Test
    @DisplayName("Update film with invalid MPA throws DataIntegrityViolationException")
    void testUpdateFilmWithInvalidMpaThrowsException() {
        Film originalFilm = createTestFilmWithFullObjects("Оригинальный фильм", "Оригинальное описание", LocalDate.of(2005, 10, 20), 110, 1, null);
        int filmId = insertFilm(originalFilm);

        Film updatedFilm = createTestFilmWithIds("Обновленный фильм", "Новое описание", LocalDate.of(2006, 11, 21), 115, 999, null);
        updatedFilm.setId(filmId);

        assertThatThrownBy(() -> filmStorage.updateFilm(updatedFilm))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Update film with invalid genre throws DataIntegrityViolationException")
    void testUpdateFilmWithInvalidGenreThrowsException() {
        Film originalFilm = createTestFilmWithFullObjects("Оригинальный фильм", "Оригинальное описание", LocalDate.of(2005, 10, 20), 110, 1, null);
        int filmId = insertFilm(originalFilm);

        Film updatedFilm = createTestFilmWithIds("Обновленный фильм", "Новое описание", LocalDate.of(2006, 11, 21), 115, 1, List.of(1, 999));
        updatedFilm.setId(filmId);

        assertThatThrownBy(() -> filmStorage.updateFilm(updatedFilm))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Get film by existing ID")
    void testGetFilmByIdExisting() {
        Film filmToInsert = createTestFilmWithFullObjects("Фильм для получения", "Описание", LocalDate.of(2015, 7, 10), 130, 3, List.of(1, 3));
        int filmId = insertFilm(filmToInsert);

        User user1 = createTestUser("user1@example.com", "user1", "User One", LocalDate.of(1990, 5, 1));
        int userId1 = insertUser(user1);
        User user2 = createTestUser("user2@example.com", "user2", "User Two", LocalDate.of(1991, 6, 2));
        int userId2 = insertUser(user2);

        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", filmId, userId1);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", filmId, userId2);

        Optional<Film> filmOptional = filmStorage.getFilmById(filmId);

        assertThat(filmOptional).isPresent();
        Film retrievedFilm = filmOptional.get();

        assertThat(retrievedFilm.getId()).isEqualTo(filmId);
        assertThat(retrievedFilm.getName()).isEqualTo("Фильм для получения");
        assertThat(retrievedFilm.getMpa()).isNotNull();
        assertThat(retrievedFilm.getMpa().getId()).isEqualTo(3);
        assertThat(retrievedFilm.getMpa().getName()).isEqualTo("PG-13");
        assertThat(retrievedFilm.getGenres()).hasSize(2);
        assertThat(retrievedFilm.getGenres()).extracting(Genre::getId).containsExactly(1, 3);
        assertThat(retrievedFilm.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Мультфильм");
        assertThat(retrievedFilm.getLikes()).hasSize(2);
        assertThat(retrievedFilm.getLikes()).extracting("id").containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    @DisplayName("Get film by non-existent ID")
    void testGetFilmByIdNonExistent() {
        Optional<Film> filmOptional = filmStorage.getFilmById(999);

        assertThat(filmOptional).isEmpty();
    }

    @Test
    @DisplayName("Get film by ID without genres and likes")
    void testGetFilmByIdWithoutGenresAndLikes() {
        Film filmToInsert = createTestFilmWithFullObjects("Фильм без всего", "Описание", LocalDate.of(2018, 1, 1), 95, 1, null);
        int filmId = insertFilm(filmToInsert);

        Optional<Film> filmOptional = filmStorage.getFilmById(filmId);

        assertThat(filmOptional).isPresent();
        Film retrievedFilm = filmOptional.get();

        assertThat(retrievedFilm.getId()).isEqualTo(filmId);
        assertThat(retrievedFilm.getName()).isEqualTo("Фильм без всего");
        assertThat(retrievedFilm.getMpa()).isNotNull();
        assertThat(retrievedFilm.getMpa().getId()).isEqualTo(1);
        assertThat(retrievedFilm.getMpa().getName()).isEqualTo("G");
        assertThat(retrievedFilm.getGenres()).isEmpty();
        assertThat(retrievedFilm.getLikes()).isEmpty();
    }

    @Test
    @DisplayName("Get all films from empty database")
    void testGetFilmsEmptyDatabase() {
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM Films");

        Collection<Film> films = filmStorage.getFilms();

        assertThat(films).isEmpty();
    }

    @Test
    @DisplayName("Get all films")
    void testGetFilms() {
        Film film1 = createTestFilmWithFullObjects("Фильм 1", "Описание 1", LocalDate.of(2000, 1, 1), 100, 1, List.of(1));
        Film film2 = createTestFilmWithFullObjects("Фильм 2", "Описание 2", LocalDate.of(2010, 2, 2), 120, 2, List.of(2, 3));
        Film film3 = createTestFilmWithFullObjects("Фильм 3", "Описание 3", LocalDate.of(2020, 3, 3), 90, 3, null);

        int film1Id = insertFilm(film1);
        int film2Id = insertFilm(film2);
        int film3Id = insertFilm(film3);

        User user1 = createTestUser("u1@ex.com", "u1", "U1", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("u2@ex.com", "u2", "U2", LocalDate.of(1991, 1, 1));
        int userId1 = insertUser(user1);
        int userId2 = insertUser(user2);

        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film1Id, userId1);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film2Id, userId1);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film2Id, userId2);


        Collection<Film> films = filmStorage.getFilms();

        assertThat(films).hasSize(3);

        Map<Integer, Film> filmMap = new HashMap<>();
        for (Film film : films) {
            filmMap.put(film.getId(), film);
        }

        Film retrievedFilm1 = filmMap.get(film1Id);
        assertThat(retrievedFilm1).isNotNull();
        assertThat(retrievedFilm1.getName()).isEqualTo("Фильм 1");
        assertThat(retrievedFilm1.getMpa()).isNotNull();
        assertThat(retrievedFilm1.getMpa().getId()).isEqualTo(1);
        assertThat(retrievedFilm1.getMpa().getName()).isEqualTo("G");
        assertThat(retrievedFilm1.getGenres()).hasSize(1);
        assertThat(retrievedFilm1.getGenres()).extracting(Genre::getId).containsExactly(1);
        assertThat(retrievedFilm1.getGenres()).extracting(Genre::getName).containsExactly("Комедия");
        assertThat(retrievedFilm1.getLikes()).hasSize(1);
        assertThat(retrievedFilm1.getLikes()).extracting("id").containsExactly(userId1);

        Film retrievedFilm2 = filmMap.get(film2Id);
        assertThat(retrievedFilm2).isNotNull();
        assertThat(retrievedFilm2.getName()).isEqualTo("Фильм 2");
        assertThat(retrievedFilm2.getMpa()).isNotNull();
        assertThat(retrievedFilm2.getMpa().getId()).isEqualTo(2);
        assertThat(retrievedFilm2.getMpa().getName()).isEqualTo("PG");
        assertThat(retrievedFilm2.getGenres()).hasSize(2);
        assertThat(retrievedFilm2.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(retrievedFilm2.getGenres()).extracting(Genre::getName).containsExactly("Драма", "Мультфильм");
        assertThat(retrievedFilm2.getLikes()).hasSize(2);
        assertThat(retrievedFilm2.getLikes()).extracting("id").containsExactlyInAnyOrder(userId1, userId2);

        Film retrievedFilm3 = filmMap.get(film3Id);
        assertThat(retrievedFilm3).isNotNull();
        assertThat(retrievedFilm3.getName()).isEqualTo("Фильм 3");
        assertThat(retrievedFilm3.getMpa()).isNotNull();
        assertThat(retrievedFilm3.getMpa().getId()).isEqualTo(3);
        assertThat(retrievedFilm3.getMpa().getName()).isEqualTo("PG-13");
        assertThat(retrievedFilm3.getGenres()).isEmpty();
        assertThat(retrievedFilm3.getLikes()).isEmpty();
    }

    @Test
    @DisplayName("Add and remove like")
    void testAddRemoveAndCheckLike() {
        Film film = createTestFilmWithFullObjects("Фильм с лайком", "Описание", LocalDate.now(), 100, 1, null);
        int filmId = insertFilm(film);

        User user = createTestUser("likeuser@ex.com", "likeuser", "Like User", LocalDate.of(1995, 1, 1));
        int userId = insertUser(user);

        assertThat(filmStorage.checkLikeExists(filmId, userId)).isFalse();

        filmStorage.addLike(filmId, userId);

        assertThat(filmStorage.checkLikeExists(filmId, userId)).isTrue();
        Integer likeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND like_user_id = ?", Integer.class, filmId, userId);
        assertThat(likeCount).isEqualTo(1);

        filmStorage.removeLike(filmId, userId);

        assertThat(filmStorage.checkLikeExists(filmId, userId)).isFalse();
        likeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND like_user_id = ?", Integer.class, filmId, userId);
        assertThat(likeCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Remove non-existent like does not throw exception")
    void testRemoveNonExistentLike() {
        Film film = createTestFilmWithFullObjects("Фильм без лайка", "Описание", LocalDate.now(), 100, 1, null);
        int filmId = insertFilm(film);

        User user = createTestUser("user_without_like@ex.com", "user_without_like", "No Like User", LocalDate.of(1995, 1, 1));
        int userId = insertUser(user);

        assertThat(filmStorage.checkLikeExists(filmId, userId)).isFalse();

        assertThatCode(() -> filmStorage.removeLike(filmId, userId)).doesNotThrowAnyException();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND like_user_id = ?", Integer.class, filmId, userId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Get top films by likes")
    void testGetTopFilms() {
        Film film1 = createTestFilmWithFullObjects("Film A (2 likes)", "Desc A", LocalDate.of(2000, 1, 1), 100, 1, List.of(1));
        Film film2 = createTestFilmWithFullObjects("Film B (3 likes)", "Desc B", LocalDate.of(2005, 1, 1), 110, 2, List.of(2, 3));
        Film film3 = createTestFilmWithFullObjects("Film C (1 like)", "Desc C", LocalDate.of(2010, 1, 1), 120, 3, List.of(4));
        Film film4 = createTestFilmWithFullObjects("Film D (0 likes)", "Desc D", LocalDate.of(2015, 1, 1), 130, 4, null);
        Film film5 = createTestFilmWithFullObjects("Film E (3 likes)", "Desc E", LocalDate.of(2005, 6, 1), 200, 2, List.of(2, 3));

        int film1Id = insertFilm(film1);
        int film2Id = insertFilm(film2);
        int film3Id = insertFilm(film3);
        int film4Id = insertFilm(film4);
        int film5Id = insertFilm(film5);

        User u1 = createTestUser("u1@a.com", "u1", "U1", LocalDate.of(1990, 1, 1));
        User u2 = createTestUser("u2@a.com", "u2", "U2", LocalDate.of(1990, 1, 2));
        User u3 = createTestUser("u3@a.com", "u3", "U3", LocalDate.of(1990, 1, 3));
        User u4 = createTestUser("u4@a.com", "u4", "U4", LocalDate.of(1990, 1, 4));

        int u1Id = insertUser(u1);
        int u2Id = insertUser(u2);
        int u3Id = insertUser(u3);
        int u4Id = insertUser(u4);


        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film1Id, u1Id);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film1Id, u2Id);

        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film2Id, u1Id);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film2Id, u2Id);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film2Id, u3Id);

        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film3Id, u4Id);

        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film5Id, u1Id);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film5Id, u2Id);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film5Id, u3Id);


        List<Film> topFilms = filmStorage.getTopFilms(3);

        assertThat(topFilms).hasSize(3);

        assertThat(topFilms.get(0).getLikes()).hasSize(3);
        assertThat(topFilms.get(1).getLikes()).hasSize(3);
        assertThat(topFilms.get(2).getLikes()).hasSize(2);

        List<Integer> topFilmIds = topFilms.stream().map(Film::getId).toList();
        assertThat(topFilmIds).containsExactly(film2Id < film5Id ? film2Id : film5Id, film2Id < film5Id ? film5Id : film2Id, film1Id);

        assertThat(topFilms).extracting(Film::getId).doesNotContain(film3Id, film4Id);

        Film retrievedFilmBOrE1 = topFilms.get(0);
        assertThat(retrievedFilmBOrE1.getMpa()).isNotNull();
        assertThat(retrievedFilmBOrE1.getMpa().getId()).isEqualTo(2);
        assertThat(retrievedFilmBOrE1.getMpa().getName()).isEqualTo("PG");
        assertThat(retrievedFilmBOrE1.getGenres()).hasSize(2);
        assertThat(retrievedFilmBOrE1.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(retrievedFilmBOrE1.getGenres()).extracting(Genre::getName).containsExactly("Драма", "Мультфильм");
        assertThat(retrievedFilmBOrE1.getLikes()).extracting("id").containsExactlyInAnyOrder(u1Id, u2Id, u3Id);


        Film retrievedFilmBOrE2 = topFilms.get(1);
        assertThat(retrievedFilmBOrE2.getMpa()).isNotNull();
        assertThat(retrievedFilmBOrE2.getMpa().getId()).isEqualTo(2);
        assertThat(retrievedFilmBOrE2.getMpa().getName()).isEqualTo("PG");
        assertThat(retrievedFilmBOrE2.getGenres()).hasSize(2);
        assertThat(retrievedFilmBOrE2.getGenres()).extracting(Genre::getId).containsExactly(2, 3);
        assertThat(retrievedFilmBOrE2.getGenres()).extracting(Genre::getName).containsExactly("Драма", "Мультфильм");
        assertThat(retrievedFilmBOrE2.getLikes()).extracting("id").containsExactlyInAnyOrder(u1Id, u2Id, u3Id);

        Film retrievedFilmA = topFilms.get(2);
        assertThat(retrievedFilmA.getMpa()).isNotNull();
        assertThat(retrievedFilmA.getMpa().getId()).isEqualTo(1);
        assertThat(retrievedFilmA.getMpa().getName()).isEqualTo("G");
        assertThat(retrievedFilmA.getGenres()).hasSize(1);
        assertThat(retrievedFilmA.getGenres()).extracting(Genre::getId).containsExactly(1);
        assertThat(retrievedFilmA.getGenres()).extracting(Genre::getName).containsExactly("Комедия");
        assertThat(retrievedFilmA.getLikes()).extracting("id").containsExactlyInAnyOrder(u1Id, u2Id);
    }

    @Test
    @DisplayName("Get top films when fewer films than requested count")
    void testGetTopFilmsLessThanCount() {
        Film film1 = createTestFilmWithFullObjects("Film 1", "Desc 1", LocalDate.of(2000, 1, 1), 100, 1, null);
        Film film2 = createTestFilmWithFullObjects("Film 2", "Desc 2", LocalDate.of(2005, 1, 1), 110, 2, null);
        int film1Id = insertFilm(film1);
        int film2Id = insertFilm(film2);

        User u1 = createTestUser("u1@b.com", "u1", "U1", LocalDate.of(1990, 1, 1));
        int u1Id = insertUser(u1);
        jdbcTemplate.update("INSERT INTO film_likes (film_id, like_user_id) VALUES (?, ?)", film1Id, u1Id);

        List<Film> topFilms = filmStorage.getTopFilms(5);

        assertThat(topFilms).hasSize(2);
        assertThat(topFilms.get(0).getId()).isEqualTo(film1Id);
        assertThat(topFilms.get(1).getId()).isEqualTo(film2Id);

        assertThat(topFilms.get(0).getMpa()).isNotNull();
        assertThat(topFilms.get(0).getMpa().getId()).isEqualTo(1);
        assertThat(topFilms.get(0).getMpa().getName()).isEqualTo("G");
        assertThat(topFilms.get(0).getGenres()).isEmpty();
        assertThat(topFilms.get(0).getLikes()).hasSize(1);
        assertThat(topFilms.get(0).getLikes()).extracting("id").containsExactly(u1Id);

        assertThat(topFilms.get(1).getMpa()).isNotNull();
        assertThat(topFilms.get(1).getMpa().getId()).isEqualTo(2);
        assertThat(topFilms.get(1).getMpa().getName()).isEqualTo("PG");
        assertThat(topFilms.get(1).getGenres()).isEmpty();
        assertThat(topFilms.get(1).getLikes()).isEmpty();
    }

    @Test
    @DisplayName("Get top films from empty database")
    void testGetTopFilmsEmptyDatabase() {
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM Films");

        List<Film> topFilms = filmStorage.getTopFilms(10);
        assertThat(topFilms).isEmpty();
    }

    @Test
    @DisplayName("Get top 0 films returns empty list")
    void testGetTopFilmsZeroCount() {
        Film film1 = createTestFilmWithFullObjects("Film 1", "Desc 1", LocalDate.of(2000, 1, 1), 100, 1, null);
        Film film2 = createTestFilmWithFullObjects("Film 2", "Desc 2", LocalDate.of(2005, 1, 1), 110, 2, null);
        insertFilm(film1);
        insertFilm(film2);

        List<Film> topFilms = filmStorage.getTopFilms(0);
        assertThat(topFilms).isEmpty();
    }
}