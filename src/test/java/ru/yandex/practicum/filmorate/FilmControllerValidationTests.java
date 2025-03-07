package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerValidationTests {

    private FilmController filmController;
    private Film validFilm;
    private static final LocalDate FILM_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
        validFilm = new Film();
        validFilm.setName("Test Film");
        validFilm.setDescription("Test Description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);
    }

    @Test
    void createValidFilm_shouldCreateFilmSuccessfully() {
        Film createdFilm = filmController.create(validFilm);
        assertNotNull(createdFilm.getId(), "Created film should have a non-null ID");
        assertEquals(validFilm.getName(), createdFilm.getName(), "Created film should have the same name as the input film");
    }

    @Test
    void createFilm_whenNameIsBlank_shouldThrowValidationException() {
        validFilm.setName(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a blank name should throw a ValidationException");
        assertEquals("Film name cannot be blank.", exception.getMessage(), "Exception message should indicate the name is blank");
    }

    @Test
    void createFilm_whenNameIsNull_shouldThrowValidationException() {
        validFilm.setName(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a null name should throw a ValidationException");
        assertEquals("Film name cannot be blank.", exception.getMessage(), "Exception message should indicate the name is blank");
    }

    @Test
    void createFilm_whenDescriptionIsTooLong_shouldThrowValidationException() {
        validFilm.setDescription("a".repeat(201));
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a description longer than 200 characters should throw a ValidationException");
        assertEquals("Film description cannot be null or exceed 200 characters.", exception.getMessage(), "Exception message should indicate the description is too long");
    }

    @Test
    void createFilm_whenDescriptionIsNull_shouldCreateFilmSuccessfully() {
        validFilm.setDescription(null);
        Film createdFilm = filmController.create(validFilm);
        assertNotNull(createdFilm.getId());
        assertNull(createdFilm.getDescription());
    }

    @Test
    void createFilm_whenDescriptionIs200Char_shouldCreateFilmSuccessfully() {
        validFilm.setDescription("a".repeat(200));
        Film createdFilm = filmController.create(validFilm);
        assertNotNull(createdFilm.getId(), "Created film should have a non-null ID");
        assertEquals(validFilm.getDescription(), createdFilm.getDescription(), "Created film should have the same description as the input film");
    }

    @Test
    void createFilm_whenReleaseDateIsBeforeFilmBirthday_shouldThrowValidationException() {
        validFilm.setReleaseDate(FILM_BIRTHDAY.minusDays(1));
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a release date before the film birthday should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Film release date cannot be null or before"), "Exception message should indicate the release date is invalid");
    }

    @Test
    void createFilm_whenReleaseDateIsFilmBirthday_shouldCreateFilmSuccessfully() {
        validFilm.setReleaseDate(FILM_BIRTHDAY);
        Film createdFilm = filmController.create(validFilm);
        assertNotNull(createdFilm.getId(), "Created film should have a non-null ID");
        assertEquals(validFilm.getReleaseDate(), createdFilm.getReleaseDate(), "Created film should have the same release date as the input film");
    }

    @Test
    void createFilm_whenReleaseDateIsNull_shouldThrowValidationException() {
        validFilm.setReleaseDate(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a null release date should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Film release date cannot be null or before"), "Exception message should indicate release date is null");
    }

    @Test
    void createFilm_whenDurationIsNegative_shouldThrowValidationException() {
        validFilm.setDuration(-1);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a negative duration should throw a ValidationException");
        assertEquals("Film duration must be positive.", exception.getMessage(), "Exception message should indicate the duration is negative");
    }

    @Test
    void createFilm_whenDurationIsZero_shouldThrowValidationException() {
        validFilm.setDuration(0);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.create(validFilm),
                "Creating a film with a zero duration should throw a ValidationException");
        assertEquals("Film duration must be positive.", exception.getMessage(), "Exception message should indicate the duration is zero");
    }

    @Test
    void updateFilm_whenFilmIsValid_shouldUpdateFilmSuccessfully() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setName("Updated Film Name");
        Film updatedFilm = filmController.update(createdFilm);
        assertEquals("Updated Film Name", updatedFilm.getName(), "Updated film should have the new name");
    }

    @Test
    void updateFilm_whenIdIsInvalid_shouldThrowValidationException() {
        validFilm.setId(9999);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(validFilm),
                "Updating a film with an invalid ID should throw ValidationException");
        assertEquals("Film with ID 9999 not found.", exception.getMessage());
    }

    @Test
    void updateFilm_whenNameIsBlank_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setName(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a blank name should throw a ValidationException");
        assertEquals("Film name cannot be blank.", exception.getMessage(), "Exception message should indicate the name is blank");
    }

    @Test
    void updateFilm_whenNameIsNull_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setName(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a null name should throw a ValidationException");
        assertEquals("Film name cannot be blank.", exception.getMessage(), "Exception message should indicate the name is null");
    }

    @Test
    void updateFilm_whenDescriptionIsTooLong_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setDescription("a".repeat(201));
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a description longer than 200 characters should throw a ValidationException");
        assertEquals("Film description cannot be null or exceed 200 characters.", exception.getMessage(), "Exception message should indicate the description is too long");
    }

    @Test
    void updateFilm_whenDescriptionIs200Char_shouldUpdateFilmSuccessfully() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setDescription("a".repeat(200));
        Film updatedFilm = filmController.update(createdFilm);
        assertEquals(createdFilm.getDescription(), updatedFilm.getDescription(), "Updated film should have the same description as the input film");
    }

    @Test
    void updateFilm_whenReleaseDateIsBeforeFilmBirthday_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setReleaseDate(FILM_BIRTHDAY.minusDays(1));
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a release date before the film birthday should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Film release date cannot be null or before"), "Exception message should indicate the release date is invalid");
    }

    @Test
    void updateFilm_whenReleaseDateIsNull_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setReleaseDate(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a null release date should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Film release date cannot be null or before"), "Exception message should indicate release date is null");

    }

    @Test
    void updateFilm_whenDurationIsNegative_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setDuration(-1);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a negative duration should throw a ValidationException");
        assertEquals("Film duration must be positive.", exception.getMessage(), "Exception message should indicate the duration is negative");
    }

    @Test
    void updateFilm_whenDurationIsZero_shouldThrowValidationException() {
        Film createdFilm = filmController.create(validFilm);
        createdFilm.setDuration(0);
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.update(createdFilm),
                "Updating a film with a zero duration should throw a ValidationException");
        assertEquals("Film duration must be positive.", exception.getMessage(), "Exception message should indicate the duration is zero");
    }
}