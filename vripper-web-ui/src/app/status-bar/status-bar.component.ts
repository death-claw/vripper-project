import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { CommonModule, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatDividerModule } from '@angular/material/divider';
import { DownloadSpeedPipe } from '../pipes/download-speed.pipe';
import { ApplicationEndpointService } from '../services/application-endpoint.service';

@Component({
  selector: 'app-status-bar',
  imports: [CommonModule, NgIf, MatDividerModule, DownloadSpeedPipe],
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class StatusBarComponent {
  handsetPortrait$ = this.breakpointObserver.observe(
    Breakpoints.HandsetPortrait
  );
  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private breakpointObserver: BreakpointObserver
  ) {}

  queueState$ = this.applicationEndpoint.queueState$;
  downloadSpeed$ = this.applicationEndpoint.downloadSpeed$;
  vgUsername$ = this.applicationEndpoint.vgUsername$;
  errorCount$ = this.applicationEndpoint.errorCount$;
}
