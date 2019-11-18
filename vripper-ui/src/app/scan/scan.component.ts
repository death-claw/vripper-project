import {
  Component,
  OnInit,
  NgZone,
  ViewChild,
  Inject,
  ChangeDetectionStrategy,
  EventEmitter,
  AfterViewInit
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatSnackBar, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { ServerService } from '../server-service';
import { MultiPostComponent } from '../multi-post/multi-post.component';

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

  @ViewChild(MultiPostComponent)
  multipost: MultiPostComponent;

  input: string;
  threadId: EventEmitter<string> = new EventEmitter();
  hideScan: EventEmitter<boolean> = new EventEmitter();

  submit(form: NgForm) {
    this.ngZone.run(() => {
      this.hideScan.emit(true);
      this.threadId.emit(null);
      this.processUrl(this.input, form);
    });
  }

  done(done: boolean) {
    this.ngZone.run(() => {
      if (done) {
        this.dialogRef.close();
      }
    });
  }

  processUrl(url: string, form?: NgForm) {
    this.httpClient
      .post<{ threadId: string; postId: string }>(this.serverService.baseUrl + '/post', { url: url })
      .pipe(
        finalize(() => {
          this.ngZone.run(() => {
            if (form != null) {
              form.resetForm();
              this.input = null;
            }
          });
        })
      )
      .subscribe(response => {
        this.ngZone.run(() => {
          if (response.postId != null) {
            this.httpClient
              .post(this.serverService.baseUrl + '/post/add', [response])
              .pipe(finalize(() => this.dialogRef.close()))
              .subscribe(
                () => {
                  this._snackBar.open('Adding posts to queue', null, {
                    duration: 5000
                  });
                },
                error => {
                  this._snackBar.open(error.error, null, {
                    duration: 5000
                  });
                }
              );
            return;
          }
          this.threadId.emit(response.threadId);
        });
      });
  }

  addPosts() {
    this.ngZone.run(() => {
      if (this.multipost != null) {
        this.multipost.submit();
      }
      this.dialogRef.close();
    });
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    this.ngZone.run(() => {
      if (this.data.url != null) {
        this.hideScan.emit(true);
        this.processUrl(this.data.url);
      } else {
        this.hideScan.emit(false);
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
