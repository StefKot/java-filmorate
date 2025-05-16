package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.services.FilmService;

import java.util.Collection;

@RestController
@RequestMapping("/mpa")
@Slf4j
public class MPAController {

    private final FilmService filmService;

    @Autowired
    public MPAController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<MPA> getAllMpa() {
        return filmService.getAllMpa();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MPA getMpaById(@PathVariable int id) {
        return filmService.getMpaById(id);
    }
}