import {ElectronService} from 'ngx-electron';
import {MultiPostComponent} from '../multi-post/multi-post.component';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {AgRendererComponent} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {GrabQueueState} from './grab-queue.model';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {BehaviorSubject, Observable, Subject, Subscription} from 'rxjs';
import {ServerService} from '../server-service';
import {HttpClient} from '@angular/common/http';
import {WsConnectionService} from "../ws-connection.service";

@Component({
  selector: 'app-url-cell-grab',
  templateUrl: 'url-renderer.component.html',
  styleUrls: ['url-renderer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UrlGrabRendererComponent implements OnInit, OnDestroy, AgRendererComponent {
  constructor(
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private electronService: ElectronService,
    private ws: WsConnectionService,
    private zone: NgZone,
  ) {
  }

  private grabQueue: GrabQueueState;
  private params: ICellRendererParams;
  private isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

  grabQueue$: Subject<GrabQueueState> = new BehaviorSubject(null);

  private stateSub: Subscription;
  private queueSub: Subscription;

  ngOnInit(): void {
    this.stateSub = this.ws.state.subscribe(state => {
      if (state) {
        this.queueSub = this.ws.subscribeForGrabQueue().subscribe(e => {
          this.zone.run(() => {
            e.forEach(v => {
              if (this.grabQueue.threadId === v.threadId) {
                this.grabQueue = v;
                this.grabQueue$.next(this.grabQueue);
              }
            });
          });
        });
      } else if (this.queueSub != null) {
        this.queueSub.unsubscribe();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.queueSub != null) {
      this.queueSub.unsubscribe();
    }
    if (this.stateSub != null) {
      this.stateSub.unsubscribe();
    }
  }

  agInit(params: ICellRendererParams): void {
    this.grabQueue = params.data;
    this.grabQueue$.next(this.grabQueue);
  }

  refresh(params: ICellRendererParams): boolean {
    return false;
  }

  grab() {
    const dialogRef = this.dialog.open(MultiPostComponent, {
      width: '90%',
      height: '90%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: this.grabQueue
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

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.grabQueue.link);
    } else {
      window.open(this.grabQueue.link, '_blank');
    }
  }

  remove() {
    this.httpClient.post(this.serverService.baseUrl + '/grab/remove', {threadId: this.grabQueue.threadId}).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }
}
