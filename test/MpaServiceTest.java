package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.services.MPAService;
import ru.yandex.practicum.filmorate.storage.interfaces.MPAStorage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MpaServiceTest {

    @Mock
    private MPAStorage mockMpaStorage;

    @InjectMocks
    private MPAService mpaService;

    @Test
    @DisplayName("Get all MPA ratings when storage returns data")
    void testGetAllMpa() {
        MPA mpa1 = new MPA();
        mpa1.setId(1);
        mpa1.setName("G");
        MPA mpa2 = new MPA();
        mpa2.setId(2);
        mpa2.setName("PG");
        List<MPA> mpaListFromStorage = List.of(mpa1, mpa2);

        when(mockMpaStorage.getAllMpa()).thenReturn(mpaListFromStorage);

        Collection<MPA> resultMpaList = mpaService.getAllMpa();

        assertThat(resultMpaList).isNotNull().hasSize(2);
        assertThat(resultMpaList).containsExactlyInAnyOrder(mpa1, mpa2);

        verify(mockMpaStorage, times(1)).getAllMpa();
    }

    @Test
    @DisplayName("Get all MPA ratings when storage returns empty list")
    void testGetAllMpaEmpty() {
        List<MPA> mpaListFromStorage = List.of();

        when(mockMpaStorage.getAllMpa()).thenReturn(mpaListFromStorage);

        Collection<MPA> resultMpaList = mpaService.getAllMpa();

        assertThat(resultMpaList).isNotNull().isEmpty();
        verify(mockMpaStorage, times(1)).getAllMpa();
    }


    @Test
    @DisplayName("Get MPA rating by existing ID when storage returns data")
    void testGetMpaByIdExisting() {
        int mpaId = 3;
        MPA expectedMpa = new MPA();
        expectedMpa.setId(mpaId);
        expectedMpa.setName("PG-13");

        when(mockMpaStorage.getMpaById(mpaId)).thenReturn(Optional.of(expectedMpa));

        MPA resultMpa = mpaService.getMpaById(mpaId);

        assertThat(resultMpa).isNotNull().isEqualTo(expectedMpa);
        verify(mockMpaStorage, times(1)).getMpaById(mpaId);
    }

    @Test
    @DisplayName("Get MPA rating by non-existent ID throws NotFoundException")
    void testGetMpaByIdNonExistent() {
        int nonExistentId = 999;
        when(mockMpaStorage.getMpaById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mpaService.getMpaById(nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA not found");

        verify(mockMpaStorage, times(1)).getMpaById(nonExistentId);
    }
}