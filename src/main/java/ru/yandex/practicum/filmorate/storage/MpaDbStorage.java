package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.interfaces.MPAStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class MpaDbStorage implements MPAStorage {

    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<MPA> getAllMpa() {
        log.debug("Attempting to retrieve all MPA ratings from database");
        String sql = "SELECT * FROM MPA";
        List<MPA> mpaList = jdbcTemplate.query(sql, this::mapRowToMpa);
        log.debug("Retrieved {} MPA ratings from database", mpaList.size());
        return mpaList.stream()
                .sorted(Comparator.comparing(MPA::getId))
                .toList();
    }

    @Override
    public Optional<MPA> getMpaById(int id) {
        log.debug("Attempting to retrieve MPA rating by ID {} from database", id);
        String sql = "SELECT * FROM MPA WHERE id = ?";
        Optional<MPA> mpa = jdbcTemplate.query(sql, this::mapRowToMpa, id)
                .stream()
                .findFirst();
        if (mpa.isPresent()) {
            log.debug("MPA rating with ID {} found", id);
        } else {
            log.debug("MPA rating with ID {} not found in database", id);
        }
        return mpa;
    }

    private MPA mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        MPA mpa = new MPA();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}