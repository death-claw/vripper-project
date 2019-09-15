import { Component, OnInit, NgZone, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatSnackBar, MatDialog, MatDialogRef } from '@angular/material';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { ServerService } from '../server-service';
import { MultiPostComponent } from '../multi-post/multi-post.component';

@Component({
  selector: 'app-scan',
  templateUrl: './scan.component.html',
  styleUrls: ['./scan.component.scss']
})
export class ScanComponent implements OnInit {

  constructor(
    private ngZone: NgZone,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ScanComponent>,
  ) { }

  @ViewChild(MultiPostComponent)
  multipost: MultiPostComponent;

  input: string;
  threadId: string;

  submit(form: NgForm) {
    this.threadId = null;
    this.processUrl(this.input, form);
  }

  done(done: boolean) {
    if(done) {
      this.dialogRef.close();
    }
  }

  processUrl(url: string, form?: NgForm) {
    this.ngZone.run(() => {
      // this.loading = true;
    });
    this.httpClient
      .post<{ threadId: string, postId: string}>(this.serverService.baseUrl + '/post', { url: url })
      .pipe(finalize(() => {
        this.ngZone.run(() => {
          // this.loading = false;
        });
        if (form != null) {
          form.resetForm();
          this.input = null;
        }
      }))
      .subscribe(response => {
        if (response.postId != null) {
          this.httpClient.post(this.serverService.baseUrl + '/post/add', [response]).subscribe(
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
        this.threadId = response.threadId;
      });
  }

  addPosts() {
    if (this.multipost != null) {
      this.multipost.submit();
    }
    this.dialogRef.close();
  }

  ngOnInit() {
  }

}
