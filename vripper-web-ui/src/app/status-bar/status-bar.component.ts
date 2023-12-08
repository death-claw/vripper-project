import { Component } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { MatDividerModule } from '@angular/material/divider';
import { DownloadSpeedPipe } from '../pipes/download-speed.pipe';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({
  selector: 'app-status-bar',
  standalone: true,
  imports: [CommonModule, NgIf, MatDividerModule, DownloadSpeedPipe],
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
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
