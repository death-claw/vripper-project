import { Component } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-status-bar',
  standalone: true,
  imports: [CommonModule, NgIf, MatDividerModule],
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
})
export class StatusBarComponent {
  constructor(private applicationEndpoint: ApplicationEndpointService) {}

  globalState$ = this.applicationEndpoint.globalState$;
}
