package ru.yandex.practicum.filmorate.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.GenreStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.MPAStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmStorage mockFilmStorage;

    @Mock
    private UserStorage mockUserStorage;

    @Mock
    private FilmDbStorage mockFilmDbStorage;

    @Mock
    private GenreStorage mockGenreStorage;

    @Mock
    private MPAStorage mockMpaStorage;

    @InjectMocks
    private FilmService filmService;

    private Film validFilm;
    private MPA validMpa;
    private Genre validGenre1;
    private Genre validGenre2;

    @BeforeEach
    void setUp() {
        validMpa = new MPA();
        validMpa.setId(1);
        validMpa.setName("G");

        validGenre1 = new Genre();
        validGenre1.setId(1);
        validGenre1.setName("Комедия");

        validGenre2 = new Genre();
        validGenre2.setId(2);
        validGenre2.setName("Драма");


        validFilm = new Film();
        validFilm.setId(1);
        validFilm.setName("Valid Film");
        validFilm.setDescription("Valid description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(100);
        validFilm.setMpa(validMpa);
        validFilm.setGenres(new ArrayList<>(List.of(validGenre1, validGenre2)));
        validFilm.setLikes(new java.util.HashSet<>());
    }

    @Test
    @DisplayName("Create film with invalid MPA ID throws NotFoundException")
    void testCreateFilmWithInvalidMpaIdThrowsNotFoundException() {
        Film filmWithInvalidMpa = new Film();
        filmWithInvalidMpa.setName("Film Invalid MPA");
        filmWithInvalidMpa.setDescription("Description");
        filmWithInvalidMpa.setReleaseDate(LocalDate.of(2020, 1, 1));
        filmWithInvalidMpa.setDuration(120);
        MPA invalidMpa = new MPA();
        invalidMpa.setId(999);
        filmWithInvalidMpa.setMpa(invalidMpa);
        filmWithInvalidMpa.setGenres(new ArrayList<>());
        filmWithInvalidMpa.setLikes(new java.util.HashSet<>());


        when(mockMpaStorage.getMpaById(invalidMpa.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filmService.create(filmWithInvalidMpa))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA with ID " + invalidMpa.getId() + " not found");

        verify(mockFilmStorage, never()).addFilm(any(Film.class));
    }

    @Test
    @DisplayName("Create film with invalid Genre ID throws NotFoundException")
    void testCreateFilmWithInvalidGenreIdThrowsNotFoundException() {
        Film filmWithInvalidGenre = new Film();
        filmWithInvalidGenre.setName("Film Invalid Genre");
        filmWithInvalidGenre.setDescription("Description");
        filmWithInvalidGenre.setReleaseDate(LocalDate.of(2020, 1, 1));
        filmWithInvalidGenre.setDuration(120);
        filmWithInvalidGenre.setMpa(validMpa);
        Genre invalidGenre = new Genre();
        invalidGenre.setId(999);
        filmWithInvalidGenre.setGenres(new ArrayList<>(List.of(validGenre1, invalidGenre)));
        filmWithInvalidGenre.setLikes(new java.util.HashSet<>());


        when(mockGenreStorage.getGenreById(validGenre1.getId())).thenReturn(Optional.of(validGenre1));
        when(mockGenreStorage.getGenreById(invalidGenre.getId())).thenReturn(Optional.empty());
        when(mockMpaStorage.getMpaById(validMpa.getId())).thenReturn(Optional.of(validMpa));


        assertThatThrownBy(() -> filmService.create(filmWithInvalidGenre))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Genre with ID " + invalidGenre.getId() + " not found");

        verify(mockFilmStorage, never()).addFilm(any(Film.class));
        verify(mockMpaStorage, times(1)).getMpaById(validMpa.getId());
        verify(mockGenreStorage, times(1)).getGenreById(validGenre1.getId());
        verify(mockGenreStorage, times(1)).getGenreById(invalidGenre.getId());
    }
}