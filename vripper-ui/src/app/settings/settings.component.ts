import { AppService } from './../app.service';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormGroup, FormControl } from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { ServerService } from '../server-service';
import { ElectronService } from 'ngx-electron';
import { Settings } from '../common/settings.model';
import { OpenDialogReturnValue } from 'electron';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private appService: AppService
  ) {}

  generalSettingsForm = new FormGroup({
    downloadPath: new FormControl(''),
    maxThreads: new FormControl(''),
    autoStart: new FormControl(false),
    forceOrder: new FormControl(false),
    subLocation: new FormControl(false),
    clearCompleted: new FormControl(false),
    vLogin: new FormControl(false),
    vUsername: new FormControl(''),
    vPassword: new FormControl(''),
    vThanks: new FormControl(false),
  });

  desktopSettingsForm = new FormGroup({
    desktopClipboard: new FormControl(false)
  });

  darkTheme = false;

  updateTheme() {
    this.appService.updateTheme(this.darkTheme);
  }

  ngOnInit() {
    this.darkTheme = this.appService.darkTheme;
    this.httpClient.get<Settings>(this.serverService.baseUrl + '/settings').subscribe(
      data => {
        this.generalSettingsForm.reset(data);
        this.desktopSettingsForm.reset(data);
      },
      error => {
        console.error(error);
      }
    );
  }

  browse() {
    this.electronService.remote.dialog.showOpenDialog(
      this.electronService.remote.getCurrentWindow(),
      {
        properties: ['openDirectory']
      }
    ).then((value: OpenDialogReturnValue) => {
      if (value.filePaths !== undefined) {
        this.generalSettingsForm.get('downloadPath').setValue(value.filePaths[0]);
        this.generalSettingsForm.get('downloadPath').markAsDirty();
        this.generalSettingsForm.get('downloadPath').markAsTouched();
      }
    });

  }

  onSubmit(): void {
    this.httpClient
      .post<Settings>(this.serverService.baseUrl + '/settings', {
        ...this.generalSettingsForm.value,
        ...this.desktopSettingsForm.value
      })
      .subscribe(
        data => {
          this._snackBar.open('Settings updated', null, {
            duration: 5000
          });
          this.generalSettingsForm.reset(data);
          this.desktopSettingsForm.reset(data);
          this.clipboardService.init(data);
        },
        error => {
          this._snackBar.open(error.error, null, {
            duration: 5000
          });
        }
      );
  }
}
