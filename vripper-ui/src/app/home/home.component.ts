import { Component, OnInit, HostBinding } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from 'src/environments/environment';
import { finalize } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar
  ) { }

  loading = false;
  input: string;

  ngOnInit() {
  }

  submit(form: NgForm) {
    this.loading = true;
    this.httpClient
      .post(environment.localhost + '/post', { url: this.input })
      .pipe(finalize(() => {
        this.loading = false;
        this.input = null;
        form.resetForm();
      }))
      .subscribe(data => {
      }, error => {
        this._snackBar.open(error.error, null, {
          duration: 5000,
        });
      });
  }

}
