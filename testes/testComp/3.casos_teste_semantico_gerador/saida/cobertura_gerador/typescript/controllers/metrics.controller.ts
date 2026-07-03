import { Controller, Get } from '@nestjs/common';

@Controller()
export class MetricsController {

  @Get('/metrics')
  getMetrics(): number {
    throw new Error('Not implemented');
  }
}
