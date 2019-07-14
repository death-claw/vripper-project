import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { ParseResponse } from './../common/parse-response.model';
import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, filter, flatMap } from 'rxjs/operators';
import { MatSnackBar, MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material';
import { NgForm } from '@angular/forms';
import { ServerService } from '../server-service';
import { RemoveAllResponse } from '../common/remove-all-response.model';
import { PostsComponent } from '../posts/posts.component';
import { ConfirmDialogComponent } from '../common/confirmation-component/confirmation-dialog';

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
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    public electronService: ElectronService
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

  @ViewChild(PostsComponent)
  private postsComponent: PostsComponent;

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
    this.httpClient.post<RemoveAllResponse>(this.serverService.baseUrl + '/post/clear/all', {}).subscribe(
      data => {
        this.postsComponent.removeRows(data.postIds);
        this._snackBar.open(`${data.removed} items cleared`, null, { duration: 5000 });
      },
      error => {
        this._snackBar.open(error.error, null, {
          duration: 5000
        });
      }
    );
  }

  remove() {

    this.dialog
    .open(ConfirmDialogComponent, {
      maxHeight: '100vh',
      maxWidth: '100vw',
      height: '200px',
      width: '60%',
      data: {header: 'Confirmation', content: 'Are you sure you want to remove all threads ?'}
    })
    .afterClosed()
    .pipe(
      filter(e => e === 'yes'),
      flatMap(e => this.httpClient.post<RemoveAllResponse>(this.serverService.baseUrl + '/post/remove/all', {}))
    )
    .subscribe(
      data => {
        this.postsComponent.removeRows(data.postIds);
        this._snackBar.open(`${data.removed} items removed`, null, { duration: 5000 });
      },
      error => {
        this._snackBar.open(error.error, null, {
          duration: 5000
        });
      }
    );
  }
}
