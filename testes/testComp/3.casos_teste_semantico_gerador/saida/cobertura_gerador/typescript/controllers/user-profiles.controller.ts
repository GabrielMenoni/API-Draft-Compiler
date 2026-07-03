import { Controller, Get, Put, Patch, Delete } from '@nestjs/common';
import { UserDto } from '../dtos/user.dto';

@Controller()
export class UserProfilesController {

  @Get('/user-profiles')
  getUserProfiles(): UserDto[] {
    throw new Error('Not implemented');
  }

  @Get('/user-profiles/:id')
  getUserProfilesById(): UserDto {
    throw new Error('Not implemented');
  }

  @Put('/user-profiles/:id')
  updateUserProfileById(): UserDto {
    throw new Error('Not implemented');
  }

  @Patch('/user-profiles/:id/active')
  patchUserProfileByIdActive(): boolean {
    throw new Error('Not implemented');
  }

  @Delete('/user-profiles/:id')
  deleteUserProfileById(): boolean {
    throw new Error('Not implemented');
  }
}
