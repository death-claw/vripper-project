import {
  Component,
  OnInit,
  NgZone,
  Inject,
  ChangeDetectionStrategy,
  AfterViewInit
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatSnackBar, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { ServerService } from '../server-service';
import { Subject, BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-scan',
  templateUrl: './scan.component.html',
  styleUrls: ['./scan.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ScanComponent implements OnInit, AfterViewInit {
  constructor(
    private ngZone: NgZone,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ScanComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {}

  input: string;
  loading: Subject<boolean> = new BehaviorSubject(false);

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
      .post<{ threadId: string; postId: string }>(this.serverService.baseUrl + '/post', { url: url })
      .pipe(
        finalize(() => {
          this.close();
          this.loading.next(false);
        })
      )
      .subscribe(response => {},
      error => {
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      });
  }

  ngOnInit() {}

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
