import { SharedService } from './shared.service';
import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription, Observable } from 'rxjs';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';
import { BreakpointState, Breakpoints, BreakpointObserver } from '@angular/cdk/layout';
import { MatDialog, MatSnackBar } from '@angular/material';
import { PostDetailComponent } from '../post-detail/post-detail.component';
import { HttpClient } from '@angular/common/http';
import { ServerService } from '../server-service';
import { ConfirmDialogComponent } from '../common/confirmation-component/confirmation-dialog';
import { filter, flatMap } from 'rxjs/operators';
import { RemoveResponse } from '../common/remove-response.model';
import { DownloadPath } from '../common/download-path.model';

@Component({
  selector: 'app-progress-cell',
  templateUrl: 'post-progress.renderer.component.html',
  styleUrls: ['post-progress.renderer.component.scss']
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone,
    private sharedService: SharedService,
    public electronService: ElectronService,
    private breakpointObserver: BreakpointObserver,
    public dialog: MatDialog,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
    if (this.electronService.isElectronApp) {
      this.fs = this.electronService.remote.require('fs');
    }
  }

  websocketHandlerPromise: Promise<WsHandler>;
  params: ICellRendererParams;
  postState: PostState;
  updatesSubscription: Subscription;
  expandSubscription: Subscription;
  expanded = false;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  fs;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      this.updatesSubscription = handler.subscribeForPosts(e => {
        this.zone.run(() => {
          e.forEach(v => {
            if (this.postState.postId === v.postId) {
              this.postState = v;
            }
          });
        });
      });
    });
    this.expandSubscription = this.sharedService.expandedPost.subscribe(postId => {
      if (this.expanded && this.postState.postId !== postId) {
        this.toggleExpand();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.updatesSubscription != null) {
      this.updatesSubscription.unsubscribe();

    }
    if (this.expandSubscription != null) {
      this.expandSubscription.unsubscribe();
    }
  }

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postState = params.data;
    this.params.node.setRowHeight(48);
  }

  refresh(params: ICellRendererParams): boolean {
    return false;
  }

  toggleExpand() {
    if (this.expanded) {
      this.params.node.setRowHeight(48);
    } else {
      this.params.node.setRowHeight(130);
      this.sharedService.publishExpanded(this.postState.postId);
    }
    this.params.api.onRowHeightChanged();
    this.expanded = !this.expanded;
  }

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postState.url);
    } else {
      window.open(this.postState.url, '_blank');
    }
  }

  seeDetails() {
    const dialogRef = this.dialog.open(PostDetailComponent, {
      width: '90%',
      height: '90%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: this.postState
    });

    const smallDialogSubscription = this.isExtraSmall.subscribe(result => {
      if (result.matches) {
        dialogRef.updateSize('100%', '100%');
      } else {
        dialogRef.updateSize('90%', '90%');
      }
    });

    dialogRef.afterClosed().subscribe(() => {
      if (smallDialogSubscription != null) {
        smallDialogSubscription.unsubscribe();
      }
    });
  }

  restart() {
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', { postId: this.postState.postId }).subscribe(
      () => {
        this._snackBar.open('Download started', null, {
          duration: 5000
        });
      },
      error => {
        console.error(error);
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
        data: { header: 'Confirmation', content: 'Are you sure you want to remove this item ?' }
      })
      .afterClosed()
      .pipe(
        filter(e => e === 'yes'),
        flatMap(e =>
          this.httpClient.post<RemoveResponse>(this.serverService.baseUrl + '/post/remove', {
            postId: this.postState.postId
          })
        )
      )
      .subscribe(
        data => {
          const toRemove = [];
          const nodeToDelete = this.params.api.getRowNode(data.postId);
          if (nodeToDelete != null) {
            toRemove.push(nodeToDelete.data);
          }
          this.params.api.updateRowData({ remove: toRemove });
        },
        error => {
          console.error(error);
        }
      );
  }

  open() {
    if (!this.electronService.isElectronApp) {
      console.error('Cannot open downloader folder, not electron app');
      return;
    }
    // Request the server to give the correct file location
    this.httpClient.get<DownloadPath>(this.serverService.baseUrl + '/post/path/' + this.postState.postId).subscribe(
      path => {
        if (this.fs.existsSync(path.path)) {
          this.electronService.shell.openItem(path.path);
        } else {
          if(this.postState.done <= 0) {
            this._snackBar.open('Download has not been started yet for this post', null, {
              duration: 5000
            });
          } else {
            this._snackBar.open(path.path + ' does not exist, you probably removed it', null, {
              duration: 5000
            });
          }
        }
      },
      error => {
        console.error(error);
      }
    );
  }

  stop() {
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', { postId: this.postState.postId }).subscribe(
      () => {
        this._snackBar.open('Download stopped', null, {
          duration: 5000
        });
      },
      error => {
        console.error(error);
      }
    );
  }
}
