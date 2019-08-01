import { GlobalState } from './../common/global-state.model';
import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { ParseResponse } from './../common/parse-response.model';
import { Component, OnInit, ViewChild, Inject, NgZone, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, filter, flatMap } from 'rxjs/operators';
import { MatSnackBar, MatDialog } from '@angular/material';
import { NgForm } from '@angular/forms';
import { ServerService } from '../server-service';
import { RemoveAllResponse } from '../common/remove-all-response.model';
import { PostsComponent } from '../posts/posts.component';
import { ConfirmDialogComponent } from '../common/confirmation-component/confirmation-dialog';
import { WsConnectionService } from '../ws-connection.service';
import { WsHandler } from '../ws-handler';
import { Subscription } from 'rxjs';
import { WSMessage } from '../common/ws-message.model';
import { CMD } from '../common/cmd.enum';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  @ViewChild(PostsComponent)
  private postsComponent: PostsComponent;
  loading = false;
  input: string;

  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    public electronService: ElectronService,
    private ngZone: NgZone,
    private wsConnectionService: WsConnectionService,
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscriptions: Subscription[] = [];
  globalState: GlobalState = new GlobalState(0, 0, 0, 0);

  ngOnInit() {
    this.clipboardService.links.subscribe(e => {
      this.ngZone.run(() => {
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
    });

    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to global state');
      this.subscriptions.push(
        handler.subscribeForGlobalState((e: GlobalState[]) => {
          this.ngZone.run(() => {
            this.globalState = e[0];
          });
        })
      );
      handler.send(new WSMessage(CMD.GLOBAL_STATE_SUB.toString()));
    });
  }

  submit(form: NgForm) {
    this.ngZone.run(() => {
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
    });
  }

  clear() {
    this.ngZone.run(() => {
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
    });
  }

  remove() {
    this.ngZone.run(() => {
      this.dialog
        .open(ConfirmDialogComponent, {
          maxHeight: '100vh',
          maxWidth: '100vw',
          height: '200px',
          width: '60%',
          data: { header: 'Confirmation', content: 'Are you sure you want to remove all items ?' }
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
    });
  }

  stopAll() {
    this.ngZone.run(() => {
        this.httpClient.post(this.serverService.baseUrl + '/post/stop/all', {})
        .subscribe(
          () => {
            this._snackBar.open(`Download stopped`, null, { duration: 5000 });
          },
          error => {
            this._snackBar.open(error.error, null, {
              duration: 5000
            });
          }
        );
    });
  }

  restartAll() {
    this.ngZone.run(() => {
        this.httpClient.post(this.serverService.baseUrl + '/post/restart/all', {})
        .subscribe(
          () => {
            this._snackBar.open(`Download started`, null, { duration: 5000 });
          },
          error => {
            this._snackBar.open(error.error, null, {
              duration: 5000
            });
          }
        );
    });
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.GLOBAL_STATE_UNSUB.toString()));
    });
  }
}
