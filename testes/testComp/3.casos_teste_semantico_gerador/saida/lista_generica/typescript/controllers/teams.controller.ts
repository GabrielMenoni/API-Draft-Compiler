import { Controller, Get } from '@nestjs/common';
import { TeamDto } from '../dtos/team.dto';

@Controller()
export class TeamsController {

  @Get('/teams')
  getTeams(): TeamDto[] {
    throw new Error('Not implemented');
  }
}
