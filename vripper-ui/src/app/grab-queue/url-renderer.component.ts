import { ElectronService } from 'ngx-electron';
import { MultiPostComponent } from './../multi-post/multi-post.component';
import { OnInit, OnDestroy, Component, ChangeDetectionStrategy } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { GrabQueueState } from './grab-queue.model';
import { MatDialog, MatSnackBar } from '@angular/material';
import { BreakpointObserver, BreakpointState, Breakpoints } from '@angular/cdk/layout';
import { Observable } from 'rxjs';
import { ServerService } from '../server-service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-url-cell-grab',
  template: `
    <div fxLayout="row" fxLayoutAlign="space-between center">
      <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
        ><a (click)="goTo()" href="javascript:void(0)">{{ grabQueue.link }}</a></span
      >
      <span>
        <button style="margin-right: 5px" (click)="grab()" color="primary" mat-mini-fab>
          <mat-icon>get_app</mat-icon>
        </button>
        <button (click)="remove()" color="primary" mat-mini-fab><mat-icon>delete</mat-icon></button>
      </span>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UrlGrabRendererComponent implements OnInit, OnDestroy, AgRendererComponent {
  constructor(
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private electronService: ElectronService
  ) {}

  grabQueue: GrabQueueState;
  params: ICellRendererParams;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

  ngOnInit(): void {}

  ngOnDestroy(): void {}

  agInit(params: ICellRendererParams): void {
    this.grabQueue = params.data;
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
    this.httpClient.post(this.serverService.baseUrl + '/grab/remove', { url: this.grabQueue.link }).subscribe(
      () => {},
      error => {
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }
}
