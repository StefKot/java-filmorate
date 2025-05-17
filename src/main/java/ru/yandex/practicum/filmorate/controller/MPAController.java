package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.services.MPAService;

import java.util.Collection;

@RestController
@RequestMapping("/mpa")
@Slf4j
public class MPAController {

    private final MPAService mpaService;

    @Autowired
    public MPAController(MPAService mpaService) {
        this.mpaService = mpaService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<MPA> getAllMpa() {
        log.info("MPAController: Received GET /mpa request");
        return mpaService.getAllMpa();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MPA getMpaById(@PathVariable int id) {
        log.info("MPAController: Received GET /mpa/{} request", id);
        return mpaService.getMpaById(id);
    }
}