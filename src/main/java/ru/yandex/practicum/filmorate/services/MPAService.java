package ru.yandex.practicum.filmorate.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.interfaces.MPAStorage;

import java.util.Collection;

@Slf4j
@Service
public class MPAService {

    private final MPAStorage mpaStorage;

    @Autowired
    public MPAService(MPAStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    public Collection<MPA> getAllMpa() {
        log.info("MPAService: Received request to get all MPA ratings");
        Collection<MPA> mpas = mpaStorage.getAllMpa();
        log.info("MPAService: Returning {} MPA ratings", mpas.size());
        return mpas;
    }

    public MPA getMpaById(int id) {
        log.info("MPAService: Received request to get MPA rating by ID: {}", id);
        MPA mpa = mpaStorage.getMpaById(id)
                .orElseThrow(() -> {
                    log.error("MPAService: MPA rating with ID {} not found", id);
                    return new NotFoundException("MPA not found");
                });
        log.info("MPAService: Returning MPA rating with ID: {}", id);
        return mpa;
    }
}