import { Component, Inject } from '@angular/core';
import { AsyncPipe, CommonModule, NgForOf, NgIf } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { EMPTY, Observable } from 'rxjs';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  ConnectionSettings,
  DownloadSettings,
  Settings,
  ViperSettings,
} from '../domain/settings.model';
import { ApplicationEndpointService } from '../services/application-endpoint.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatDialogModule,
    AsyncPipe,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    MatTabsModule,
    MatTooltipModule,
    NgForOf,
    NgIf,
    ReactiveFormsModule,
    MatSlideToggleModule,
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
})
export class SettingsComponent {
  mirrors: Observable<string[]> = EMPTY;

  viperGirlsSettingsForm = new FormGroup({
    login: new FormControl(false),
    username: new FormControl(''),
    password: new FormControl(''),
    thanks: new FormControl(false),
    host: new FormControl(''),
  });

  downloadSettingsForm = new FormGroup({
    downloadPath: new FormControl(''),
    tempPath: new FormControl(''),
    autoStart: new FormControl(false),
    autoQueueThreshold: new FormControl(0),
    forceOrder: new FormControl(false),
    forumSubfolder: new FormControl(false),
    threadSubLocation: new FormControl(false),
    clearCompleted: new FormControl(false),
    appendPostId: new FormControl(false),
  });

  connectionSettingsForm = new FormGroup({
    maxThreads: new FormControl(0),
    maxTotalThreads: new FormControl(0),
    timeout: new FormControl(0),
    maxAttempts: new FormControl(0),
  });

  eventLogSettingsForm = new FormGroup({
    maxEventLog: new FormControl(0),
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: Settings,
    public dialogRef: MatDialogRef<SettingsComponent>,
    private applicationEndpoint: ApplicationEndpointService
  ) {
    this.viperGirlsSettingsForm.reset(data.viperSettings);
    this.downloadSettingsForm.reset(data.downloadSettings);
    this.connectionSettingsForm.reset(data.connectionSettings);
    this.eventLogSettingsForm.reset(data);
  }

  save = () => {
    this.applicationEndpoint
      .newSettings({
        viperSettings: {
          ...(this.viperGirlsSettingsForm.value as ViperSettings),
        },
        connectionSettings: {
          ...(this.connectionSettingsForm.value as ConnectionSettings),
        },
        downloadSettings: {
          ...(this.downloadSettingsForm.value as DownloadSettings),
        },
        ...this.eventLogSettingsForm.value,
      } as Settings)
      .subscribe(() => this.dialogRef.close());
  };
}
