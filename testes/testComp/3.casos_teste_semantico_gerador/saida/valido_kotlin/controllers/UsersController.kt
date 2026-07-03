package com.example.generated.controllers

import com.example.generated.dtos.UserDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class UsersController {

    @GetMapping("/users")
    fun getUsers(): List<UserDto> {
        TODO("Not yet implemented")
    }

    @PostMapping("/users")
    fun createUser(): UserDto {
        TODO("Not yet implemented")
    }
}
