import { Component } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { MatDividerModule } from '@angular/material/divider';
import { DownloadSpeedPipe } from '../pipes/download-speed.pipe';

@Component({
  selector: 'app-status-bar',
  standalone: true,
  imports: [CommonModule, NgIf, MatDividerModule, DownloadSpeedPipe],
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
})
export class StatusBarComponent {
  constructor(private applicationEndpoint: ApplicationEndpointService) {}

  queueState$ = this.applicationEndpoint.queueState$;
  downloadSpeed$ = this.applicationEndpoint.downloadSpeed$;
  vgUsername$ = this.applicationEndpoint.vgUsername$;
  errorCount$ = this.applicationEndpoint.errorCount$;
}
