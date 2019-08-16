import { MultiPostComponent } from './../multi-post/multi-post.component';
import { GlobalState } from './../common/global-state.model';
import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit, ViewChild, Inject, NgZone, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { filter, flatMap, finalize } from 'rxjs/operators';
import { MatSnackBar, MatDialog, MatDialogRef } from '@angular/material';
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
import { DownloadSpeed } from '../common/download-speed.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  @ViewChild(PostsComponent)
  private postsComponent: PostsComponent;

  loading = false;
  spinnerMode = 'indeterminate';
  spinnerValue = 0;
  input: string;

  parseEndSubscription: Subscription;

  constructor(
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    private serverService: ServerService,
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    public electronService: ElectronService,
    private ngZone: NgZone,
    private wsConnectionService: WsConnectionService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscriptions: Subscription[] = [];
  globalState: GlobalState = new GlobalState(0, 0, 0, 0);
  downloadSpeed: DownloadSpeed = new DownloadSpeed('0 B');
  // parsingState: ThreadParsingState = new ThreadParsingState(0, 0);

  ngOnInit() {
    this.clipboardService.links.subscribe(e => {
      if (this.loading) {
        this._snackBar.open(e + ' was not processed, the app is beasy parsing another thread', null, {
          duration: 5000
        });
        return;
      }
      this.processUrl(e);
    });

    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to global state and download speed');
      this.subscriptions.push(
        handler.subscribeForGlobalState((e: GlobalState[]) => {
          this.ngZone.run(() => {
            this.globalState = e[0];
          });
        })
      );
      this.subscriptions.push(
        handler.subscribeForSpeed((e: DownloadSpeed[]) => {
          this.ngZone.run(() => {
            this.downloadSpeed = e[0];
          });
        })
      );
      handler.send(new WSMessage(CMD.GLOBAL_STATE_SUB.toString()));
      handler.send(new WSMessage(CMD.SPEED_SUB.toString()));
    });
  }

  submit(form: NgForm) {
    this.processUrl(this.input, form);
  }

  processUrl(url: string, form?: NgForm) {
    this.ngZone.run(() => {
      this.loading = true;
    });
    let dialog: MatDialogRef<MultiPostComponent>;
    this.httpClient
      .post<{ threadId: string, postId: string}>(this.serverService.baseUrl + '/post', { url: url })
      .pipe(finalize(() => {
        this.ngZone.run(() => {
          this.loading = false;
        });
        if(form != null) {
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
        dialog = this.dialog.open(MultiPostComponent, {
          maxHeight: '100vh',
          maxWidth: '100vw',
          height: '70%',
          width: '70%',
          data: { threadId: response.threadId, threadUrl: url }
        });
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
      this.httpClient.post(this.serverService.baseUrl + '/post/stop/all', {}).subscribe(
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
      this.httpClient.post(this.serverService.baseUrl + '/post/restart/all', {}).subscribe(
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
