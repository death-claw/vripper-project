import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { DownloadTableComponent } from '../download-table/download-table.component';
import { ThreadTableComponent } from '../thread-table/thread-table.component';
import { LogTableComponent } from '../log-table/log-table.component';
import { ToolbarComponent } from '../toolbar/toolbar.component';

@Component({
  selector: 'app-home-component',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatIconModule,
    MatBadgeModule,
    DownloadTableComponent,
    ThreadTableComponent,
    LogTableComponent,
    ToolbarComponent,
  ],
  templateUrl: './home-component.component.html',
  styleUrls: ['./home-component.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponentComponent {
  downloadCount = signal(0);
  threadCount = signal(0);
  logCount = signal(0);
  tabIndex = signal(0);

  onTabChange(index: number) {
    this.tabIndex.set(index);
  }
}
