import {AfterViewInit, ChangeDetectionStrategy, Component, Inject, NgZone, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpClient} from '@angular/common/http';
import {finalize} from 'rxjs/operators';
import {ServerService} from '../services/server-service';
import {BehaviorSubject, Subject} from 'rxjs';

@Component({
  selector: 'app-scan',
  templateUrl: './scan.component.html',
  styleUrls: ['./scan.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ScanComponent implements OnInit, AfterViewInit {
  input: string;
  loading: Subject<boolean> = new BehaviorSubject(false);

  constructor(
    private ngZone: NgZone,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ScanComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {
  }

  submit() {
    this.ngZone.run(() => {
      this.processUrl(this.input);
    });
  }

  done(done: boolean) {
    this.ngZone.run(() => {
      if (done) {
        this.dialogRef.close();
      }
    });
  }

  processUrl(url: string) {
    this.loading.next(true);
    this.httpClient
      .post<{ threadId: string; postId: string }>(this.serverService.baseUrl + '/post', {url: url})
      .pipe(
        finalize(() => {
          this.close();
          this.loading.next(false);
        })
      )
      .subscribe(response => {
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        });
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    this.ngZone.run(() => {
      if (this.data.url != null) {
        this.processUrl(this.data.url);
      }
    });
  }

  close() {
    this.ngZone.run(() => {
      this.dialogRef.close();
    });
  }
}

export interface DialogData {
  url: string;
}
