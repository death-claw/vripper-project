import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  Signal,
} from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ScanComponent } from '../scan/scan.component';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTooltipModule,
    MatDialogModule,
    NgIf,
    MatDividerModule,
  ],
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToolbarComponent {
  @Input({ required: true })
  selectedTab!: number;

  @Output()
  links = new EventEmitter<string>();

  @Output()
  startSelected = new EventEmitter<void>();

  @Output()
  stopSelected = new EventEmitter<void>();

  @Output()
  deleteSelected = new EventEmitter<void>();

  @Output()
  renameSelected = new EventEmitter<void>();

  @Output()
  clearDownload = new EventEmitter<void>();

  @Output()
  clearLogs = new EventEmitter<void>();

  @Output()
  clearThreads = new EventEmitter<void>();

  @Output()
  startDownload = new EventEmitter<void>();

  @Output()
  stopDownload = new EventEmitter<void>();

  @Output()
  settings = new EventEmitter<void>();

  @Input({ required: true })
  disableSelected!: Signal<boolean>;

  handsetPortrait$ = this.breakpointObserver.observe(
    Breakpoints.HandsetPortrait
  );

  constructor(
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver
  ) {}

  openScanDialog() {
    this.dialog
      .open<ScanComponent, never, string>(ScanComponent, {
        maxWidth: '100vw',
        maxHeight: '100vh',
      })
      .afterClosed()
      .subscribe(v => {
        if (v) {
          this.links.next(v);
        }
      });
  }

  startSelectedClick() {
    this.startSelected.emit();
  }

  stopSelectedClick() {
    this.stopSelected.emit();
  }

  startClick() {
    this.startDownload.emit();
  }

  stopClick() {
    this.stopDownload.emit();
  }

  settingsClick() {
    this.settings.next();
  }

  removeSelectedClick() {
    this.deleteSelected.next();
  }

  renameSelectedClick() {
    this.renameSelected.next();
  }

  clearDownloadsClick() {
    this.clearDownload.next();
  }

  clearLogsClick() {
    this.clearLogs.emit();
  }

  clearThreadsClick() {
    this.clearThreads.emit();
  }
}
