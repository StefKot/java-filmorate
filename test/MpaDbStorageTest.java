package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.MPA;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({MpaDbStorage.class})
class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;
    private final JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Get all MPA ratings")
    void testGetAllMpa() {
        Collection<MPA> mpaList = mpaStorage.getAllMpa();

        assertThat(mpaList).isNotNull();
        assertThat(mpaList).hasSize(5);
        assertThat(mpaList).extracting(MPA::getId).containsExactly(1, 2, 3, 4, 5);
        assertThat(mpaList).extracting(MPA::getName).containsExactly("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    @DisplayName("Get MPA rating by existing ID")
    void testGetMpaByIdExisting() {
        Optional<MPA> mpaOptional = mpaStorage.getMpaById(3);

        assertThat(mpaOptional).isPresent();
        MPA mpa = mpaOptional.get();
        assertThat(mpa.getId()).isEqualTo(3);
        assertThat(mpa.getName()).isEqualTo("PG-13");
    }

    @Test
    @DisplayName("Get MPA rating by non-existent ID")
    void testGetMpaByIdNonExistent() {
        Optional<MPA> mpaOptional = mpaStorage.getMpaById(999);

        assertThat(mpaOptional).isEmpty();
    }
}