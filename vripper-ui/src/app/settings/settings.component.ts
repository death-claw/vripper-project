import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { NgForm } from '@angular/forms';
import { MatSnackBar } from '@angular/material';

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
  constructor(private httpClient: HttpClient, private _snackBar: MatSnackBar) {}

  settings: Settings = {
    downloadPath: null,
    maxThreads: null,
    autoStart: false,
    vLogin: false,
    vUsername: null,
    vPassword: null,
    vThanks: false,
  };

  ngOnInit() {
    this.httpClient
      .get<Settings>(environment.localhost + '/settings')
      .subscribe(data => {
        this.settings = data;
      }, error => {
        console.error(error);
      });
  }

  onSubmit(f: NgForm): void {
    this.httpClient
      .post(environment.localhost + '/settings', this.settings)
      .subscribe(() => {
        this._snackBar.open('Settings updated', null, {
          duration: 5000,
        });
        f.resetForm(this.settings);
      }, error => {
        this._snackBar.open(error.error.message, null, {
          duration: 5000,
        });
      });
  }
}
