import { Controller, Get, Post } from '@nestjs/common';
import { UserDto } from '../dtos/user.dto';

@Controller()
export class UsersController {

  @Get('/users')
  getUsers(): UserDto[] {
    throw new Error('Not implemented');
  }

  @Post('/users')
  createUser(): UserDto {
    throw new Error('Not implemented');
  }
}
