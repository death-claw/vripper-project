import {finalize} from 'rxjs/operators';
import {AppService} from '../app.service';
import {ClipboardService} from '../clipboard.service';
import {ChangeDetectionStrategy, Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {FormControl, FormGroup} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ServerService} from '../server-service';
import {ElectronService} from 'ngx-electron';
import {Settings} from '../domain/settings.model';
import {OpenDialogReturnValue} from 'electron';
import {BehaviorSubject, Subject} from 'rxjs';

interface CacheSize {
  size: string;
}

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent implements OnInit {
  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private appService: AppService
  ) {
  }

  loading = false;

  generalSettingsForm = new FormGroup({
    downloadPath: new FormControl(''),
    maxThreads: new FormControl(''),
    maxTotalThreads: new FormControl(''),
    autoStart: new FormControl(false),
    forceOrder: new FormControl(false),
    subLocation: new FormControl(false),
    threadSubLocation: new FormControl(false),
    clearCompleted: new FormControl(false),
    vLogin: new FormControl(false),
    vUsername: new FormControl(''),
    vPassword: new FormControl(''),
    vThanks: new FormControl(false),
    viewPhotos: new FormControl(false)
  });

  desktopSettingsForm = new FormGroup({
    desktopClipboard: new FormControl(false),
    notification: new FormControl(false)
  });

  darkTheme = false;
  cacheSize: Subject<CacheSize> = new BehaviorSubject({size: '0'});
  cacheClearLoading: Subject<boolean> = new BehaviorSubject(false);

  updateTheme() {
    this.appService.updateTheme(this.darkTheme);
  }

  updateSettings(settings: Settings) {
    this.appService.updateSettings(settings);
  }

  ngOnInit() {
    this.darkTheme = this.appService.darkTheme;
    this.httpClient.get<Settings>(this.serverService.baseUrl + '/settings')
      .subscribe(data => {
        this.generalSettingsForm.reset(data);
        this.desktopSettingsForm.reset(data);
      }, error => {
        this._snackBar.open(error?.error?.message.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      });
    this.httpClient.get<CacheSize>(this.serverService.baseUrl + '/gallery/cache')
      .subscribe(data => {
        this.cacheSize.next(data);
      }, error => {
        this._snackBar.open(error?.error?.message.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      });
  }

  clearCache() {
    this.cacheClearLoading.next(true);
    this.httpClient.get<CacheSize>(this.serverService.baseUrl + '/gallery/cache/clear')
      .pipe(finalize(() => this.cacheClearLoading.next(false)))
      .subscribe(data => {
        this.cacheSize.next(data);
      }, error => {
        this._snackBar.open(error?.error?.message.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      })
  }

  browse() {
    this.electronService.remote.dialog
      .showOpenDialog(this.electronService.remote.getCurrentWindow(), {
        properties: ['openDirectory']
      })
      .then((value: OpenDialogReturnValue) => {
        if (value.filePaths !== undefined) {
          this.generalSettingsForm.get('downloadPath').setValue(value.filePaths[0]);
          this.generalSettingsForm.get('downloadPath').markAsDirty();
          this.generalSettingsForm.get('downloadPath').markAsTouched();
        }
      });
  }

  onSubmit(): void {
    this.loading = true;
    this.httpClient
      .post<Settings>(this.serverService.baseUrl + '/settings', {
        ...this.generalSettingsForm.value,
        darkTheme: this.darkTheme,
        ...this.desktopSettingsForm.value
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe(
        data => {
          this._snackBar.open('Settings updated', null, {
            duration: 5000
          });
          this.generalSettingsForm.reset(data);
          this.desktopSettingsForm.reset(data);
          this.clipboardService.init(data);
          this.updateSettings(data);
        },
        error => {
          this._snackBar.open(error?.error?.message.error || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
  }
}
