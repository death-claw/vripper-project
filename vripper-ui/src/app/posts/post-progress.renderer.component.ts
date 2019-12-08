import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import {
  Component,
  OnInit,
  OnDestroy,
  NgZone,
  ChangeDetectionStrategy,
  AfterViewInit,
  EventEmitter
} from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription, Observable, BehaviorSubject, Subject } from 'rxjs';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';
import { BreakpointState, Breakpoints, BreakpointObserver } from '@angular/cdk/layout';
import { MatDialog, MatSnackBar } from '@angular/material';
import { PostDetailComponent } from '../post-detail/post-detail.component';
import { HttpClient } from '@angular/common/http';
import { ServerService } from '../server-service';
import { DownloadPath } from '../common/download-path.model';

@Component({
  selector: 'app-progress-cell',
  templateUrl: 'post-progress.renderer.component.html',
  styleUrls: ['post-progress.renderer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy, AfterViewInit {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone,
    public electronService: ElectronService,
    private breakpointObserver: BreakpointObserver,
    public dialog: MatDialog,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
    if (this.electronService.isElectronApp) {
      this.fs = this.electronService.remote.require('fs');
    }
  }

  websocketHandlerPromise: Promise<WsHandler>;
  postState$: EventEmitter<PostState> = new EventEmitter();
  private postState: PostState;
  updatesSubscription: Subscription;
  expanded = false;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  fs;
  loaded: Subject<boolean> = new BehaviorSubject(false);
  loading;

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
              this.postState$.emit(this.postState);
            }
          });
        });
      });
    });
  }

  ngAfterViewInit(): void {
    this.postState$.emit(this.postState);
    this.loading = setTimeout(() => this.loaded.next(true), 100);
  }

  ngOnDestroy(): void {
    if (this.updatesSubscription != null) {
      this.updatesSubscription.unsubscribe();
    }
    clearTimeout(this.loading);
  }

  agInit(params: ICellRendererParams): void {
    this.postState = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.postState = params.data;
    this.postState$.emit(this.postState);
    return true;
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
          if (this.postState.done <= 0) {
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
}
