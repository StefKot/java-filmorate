package ru.yandex.practicum.filmorate.storage.interfaces;

import ru.yandex.practicum.filmorate.model.MPA;

import java.util.Collection;
import java.util.Optional;

public interface MPAStorage {

    Collection<MPA> getAllMpa();

    Optional<MPA> getMpaById(int id);
}