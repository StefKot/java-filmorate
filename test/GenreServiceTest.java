package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.services.GenreService;
import ru.yandex.practicum.filmorate.storage.interfaces.GenreStorage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreStorage mockGenreStorage;

    @InjectMocks
    private GenreService genreService;

    @Test
    @DisplayName("Get all genres when storage returns data")
    void testGetAllGenres() {
        Genre genre1 = new Genre();
        genre1.setId(1);
        genre1.setName("Комедия");
        Genre genre2 = new Genre();
        genre2.setId(2);
        genre2.setName("Драма");
        List<Genre> genresFromStorage = List.of(genre1, genre2);

        when(mockGenreStorage.getAllGenres()).thenReturn(genresFromStorage);

        Collection<Genre> resultGenres = genreService.getAllGenres();

        assertThat(resultGenres).isNotNull().hasSize(2);
        assertThat(resultGenres).containsExactlyInAnyOrder(genre1, genre2);

        verify(mockGenreStorage, times(1)).getAllGenres();
    }

    @Test
    @DisplayName("Get all genres when storage returns empty list")
    void testGetAllGenresEmpty() {
        List<Genre> genresFromStorage = List.of();

        when(mockGenreStorage.getAllGenres()).thenReturn(genresFromStorage);

        Collection<Genre> resultGenres = genreService.getAllGenres();

        assertThat(resultGenres).isNotNull().isEmpty();
        verify(mockGenreStorage, times(1)).getAllGenres();
    }


    @Test
    @DisplayName("Get genre by existing ID when storage returns data")
    void testGetGenreByIdExisting() {
        int genreId = 3;
        Genre expectedGenre = new Genre();
        expectedGenre.setId(genreId);
        expectedGenre.setName("Мультфильм");

        when(mockGenreStorage.getGenreById(genreId)).thenReturn(Optional.of(expectedGenre));

        Genre resultGenre = genreService.getGenreById(genreId);

        assertThat(resultGenre).isNotNull().isEqualTo(expectedGenre);
        verify(mockGenreStorage, times(1)).getGenreById(genreId);
    }

    @Test
    @DisplayName("Get genre by non-existent ID throws NotFoundException")
    void testGetGenreByIdNonExistent() {
        int nonExistentId = 999;
        when(mockGenreStorage.getGenreById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.getGenreById(nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Genre not found");

        verify(mockGenreStorage, times(1)).getGenreById(nonExistentId);
    }
}