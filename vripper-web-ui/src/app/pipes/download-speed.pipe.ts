import { Pipe, PipeTransform } from '@angular/core';
import { formatBytes } from '../utils/utils';

@Pipe({
  standalone: true,
  name: 'downloadSpeed',
})
export class DownloadSpeedPipe implements PipeTransform {
  transform(value: number): string {
    return formatBytes(value);
  }
}
