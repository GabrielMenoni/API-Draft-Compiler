package com.example.generated.controllers

import com.example.generated.dtos.UserDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class UserProfilesController {

    @GetMapping("/user-profiles")
    fun getUserProfiles(): List<UserDto> {
        TODO("Not yet implemented")
    }

    @GetMapping("/user-profiles/{id}")
    fun getUserProfilesById(): UserDto {
        TODO("Not yet implemented")
    }

    @PutMapping("/user-profiles/{id}")
    fun updateUserProfileById(): UserDto {
        TODO("Not yet implemented")
    }

    @PatchMapping("/user-profiles/{id}/active")
    fun patchUserProfileByIdActive(): Boolean {
        TODO("Not yet implemented")
    }

    @DeleteMapping("/user-profiles/{id}")
    fun deleteUserProfileById(): Boolean {
        TODO("Not yet implemented")
    }
}
