package com.example.generated.controllers

import com.example.generated.dtos.TeamDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class TeamsController {

    @GetMapping("/teams")
    fun getTeams(): List<TeamDto> {
        TODO("Not yet implemented")
    }
}
