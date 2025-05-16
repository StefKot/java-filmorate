package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MPA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<MPA> getAllMpa() {
        String sql = "SELECT * FROM MPA";
        List<MPA> mpaList = jdbcTemplate.query(sql, this::mapRowToMpa);
        return mpaList.stream()
                .sorted(Comparator.comparing(MPA::getId))
                .toList();
    }

    public Optional<MPA> getMpaById(int id) {
        String sql = "SELECT * FROM MPA WHERE id = ?";
        return jdbcTemplate.query(sql, this::mapRowToMpa, id)
                .stream()
                .findFirst();
    }

    private MPA mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        MPA mpa = new MPA();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}