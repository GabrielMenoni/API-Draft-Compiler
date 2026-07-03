import { UserDto } from './user.dto';

export class TeamDto {
  name!: string;
  members!: UserDto[];
}
