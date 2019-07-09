import { ClipboardService } from './../clipboard.service';
import { ParseResponse } from './../common/parse-response.model';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material';
import { NgForm } from '@angular/forms';
import { ServerService } from '../server-service';
import { RemoveAllResponse } from '../common/remove-all-response.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    private clipboardService: ClipboardService
  ) {
    this.clipboardService.links.subscribe(e => {
      this.loading = true;
      this.httpClient
        .post<ParseResponse>(this.serverService.baseUrl + '/clipboard/post', { url: e })
        .pipe(
          finalize(() => {
            this.loading = false;
          })
        )
        .subscribe(
          data => {
            this._snackBar.open(`${data.parsed} posts parsed`, null, { duration: 5000 });
          },
          error => {
            this._snackBar.open(error.error, null, {
              duration: 5000
            });
          }
        );
    });
  }

  loading = false;
  input: string;

  ngOnInit() {}

  submit(form: NgForm) {
    this.loading = true;
    this.httpClient
      .post<ParseResponse>(this.serverService.baseUrl + '/post', { url: this.input })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.input = null;
          form.resetForm();
        })
      )
      .subscribe(
        data => {
          this._snackBar.open(`${data.parsed} posts parsed`, null, { duration: 5000 });
        },
        error => {
          this._snackBar.open(error.error, null, {
            duration: 5000
          });
        }
      );
  }

  clear() {
    this.loading = true;
    this.httpClient
      .post<RemoveAllResponse>(this.serverService.baseUrl + '/post/remove/all', {})
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe(
        data => {
          this._snackBar.open(`${data.removed} items cleared`, null, { duration: 5000 });
        },
        error => {
          this._snackBar.open(error.error, null, {
            duration: 5000
          });
        }
      );
  }
}
