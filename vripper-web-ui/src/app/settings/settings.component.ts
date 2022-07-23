import {finalize} from 'rxjs/operators';
import {AppService} from '../services/app.service';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {FormControl, FormGroup} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ServerService} from '../services/server-service';
import {ConnectionSettings, DownloadSettings, Settings, ViperSettings} from '../domain/settings.model';
import {EMPTY, Observable, Subscription} from 'rxjs';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent implements OnInit, OnDestroy {
  loading = false;
  darkTheme = false;

  mirrors: Observable<string[]> = EMPTY;

  subscriptions: Subscription[] = [];

  viperGirlsSettingsForm = new FormGroup({
    login: new FormControl(false),
    username: new FormControl(''),
    password: new FormControl(''),
    thanks: new FormControl(false),
    host: new FormControl('')
  });

  downloadSettingsForm = new FormGroup({
    downloadPath: new FormControl(''),
    autoStart: new FormControl(false),
    forceOrder: new FormControl(false),
    forumSubfolder: new FormControl(false),
    threadSubLocation: new FormControl(false),
    clearCompleted: new FormControl(false),
    appendPostId: new FormControl(false)
  });

  connectionSettingsForm = new FormGroup({
    maxThreads: new FormControl(0),
    maxTotalThreads: new FormControl(0),
    timeout: new FormControl(0),
    maxAttempts: new FormControl(0)
  });

  desktopSettingsForm = new FormGroup({
    desktopClipboard: new FormControl(false)
  });

  eventLogSettingsForm = new FormGroup({
    maxEventLog: new FormControl(0)
  });

  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    private appService: AppService,
    private zone: NgZone
  ) {
  }

  updateTheme() {
    this.appService.updateTheme(this.darkTheme);
  }

  updateSettings(settings: Settings) {
    this.appService.updateSettings(settings);
  }

  ngOnInit() {
    this.darkTheme = this.appService.darkTheme;
    this.mirrors = this.httpClient.get<string[]>(this.serverService.baseUrl + '/settings/proxies');
    this.httpClient.get<Settings>(this.serverService.baseUrl + '/settings')
      .subscribe(data => {
        this.viperGirlsSettingsForm.reset(data.viperSettings);
        this.downloadSettingsForm.reset(data.downloadSettings);
        this.connectionSettingsForm.reset(data.connectionSettings);
        this.desktopSettingsForm.reset(data);
        this.eventLogSettingsForm.reset(data);
      }, error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
          duration: 5000
        });
      });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onSubmit(): void {
    this.loading = true;
    this.httpClient
      .post<Settings>(this.serverService.baseUrl + '/settings', {
        viperSettings: {...this.viperGirlsSettingsForm.value as ViperSettings},
        connectionSettings: {...this.connectionSettingsForm.value as ConnectionSettings},
        downloadSettings: {...this.downloadSettingsForm.value as DownloadSettings},
        darkTheme: this.darkTheme,
        ...this.desktopSettingsForm.value,
        ...this.eventLogSettingsForm.value
      } as Settings)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe(
        data => {
          this._snackBar.open('Settings updated', undefined, {
            duration: 5000
          });
          this.viperGirlsSettingsForm.reset(data.viperSettings);
          this.downloadSettingsForm.reset(data.downloadSettings);
          this.connectionSettingsForm.reset(data.connectionSettings);
          this.desktopSettingsForm.reset(data);
          this.eventLogSettingsForm.reset(data);
          this.updateSettings(data);
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      );
  }
}
