import { Controller, Get } from '@nestjs/common';

@Controller()
export class RootController {

  @Get('/')
  getRoot(): string {
    throw new Error('Not implemented');
  }
}
