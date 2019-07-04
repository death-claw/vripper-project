import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormGroup, FormControl } from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { ServerService } from '../server-service';
import { ElectronService } from 'ngx-electron';

export interface Settings {
  downloadPath: string;
  maxThreads: number;
  autoStart: boolean;
  vLogin: boolean;
  vUsername: string;
  vPassword: string;
  vThanks: boolean;
}

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
    public electronService: ElectronService
  ) {}

  settingsForm = new FormGroup({
    downloadPath: new FormControl(''),
    maxThreads: new FormControl(''),
    autoStart: new FormControl(false),
    vLogin: new FormControl(false),
    vUsername: new FormControl(''),
    vPassword: new FormControl(''),
    vThanks: new FormControl(false)
  });

  // settings: Settings = {
  //   downloadPath: null,
  //   maxThreads: null,
  //   autoStart: false,
  //   vLogin: false,
  //   vUsername: null,
  //   vPassword: null,
  //   vThanks: false
  // };

  ngOnInit() {
    this.httpClient.get<Settings>(this.serverService.baseUrl + '/settings').subscribe(
      data => {
        this.settingsForm.reset(data);
      },
      error => {
        console.error(error);
      }
    );
  }

  browse() {
    const result: string[] | undefined = this.electronService.remote.dialog.showOpenDialog(
      this.electronService.remote.getCurrentWindow(),
      {
        properties: ['openDirectory']
      }
    );

    if (result !== undefined) {
      this.settingsForm.get('downloadPath').setValue(result[0]);
      this.settingsForm.get('downloadPath').markAsDirty();
      this.settingsForm.get('downloadPath').markAsTouched();
    }
  }

  onSubmit(): void {
    this.httpClient.post(this.serverService.baseUrl + '/settings', this.settingsForm.value).subscribe(
      () => {
        this._snackBar.open('Settings updated', null, {
          duration: 5000
        });
        this.settingsForm.markAsUntouched();
        this.settingsForm.markAsPristine();
      },
      error => {
        this._snackBar.open(error.error.message, null, {
          duration: 5000
        });
      }
    );
  }
}
