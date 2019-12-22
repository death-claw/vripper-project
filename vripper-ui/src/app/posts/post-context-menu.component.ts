import { GalleryComponent } from './../gallery/gallery.component';
import { ServerService } from './../server-service';
import { HttpClient } from '@angular/common/http';
import { ElectronService } from 'ngx-electron';
import { Component, ChangeDetectionStrategy, NgZone } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { MatDialog, MatSnackBar } from '@angular/material';
import { PostDetailComponent } from '../post-detail/post-detail.component';
import { Observable } from 'rxjs';
import { BreakpointState, BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { PostState } from './post-state.model';
import { DownloadPath } from '../common/download-path.model';
import { ContextMenuService } from '../ctxt-menu.service';
import { ConfirmDialogComponent } from '../common/confirmation-component/confirmation-dialog';
import { filter, flatMap } from 'rxjs/operators';
import { RemoveResponse } from '../common/remove-response.model';

@Component({
  selector: 'app-post-ctx-menu',
  template: `
    <mat-card class="mat-elevation-z4">
      <mat-action-list>
        <button
          (click)="restart()"
          *ngIf="
            (postState.status === 'COMPLETE' && postState.progress !== 100) ||
            postState.status === 'ERROR' ||
            postState.status === 'STOPPED'
          "
          mat-list-item
        >
          <mat-icon>play_arrow</mat-icon>
          <span>Start</span>
        </button>
        <button
          (click)="stop()"
          *ngIf="postState.status === 'DOWNLOADING' || postState.status === 'PARTIAL' || postState.status === 'PENDING'"
          mat-list-item
        >
          <mat-icon>stop</mat-icon>
          <span>Stop</span>
        </button>
        <button (click)="remove()" mat-list-item>
          <mat-icon>delete</mat-icon>
          <span>Remove</span>
        </button>
        <button (click)="seeDetails()" mat-list-item>
          <mat-icon>list</mat-icon>
          <span>Files</span>
        </button>
        <button (click)="openGallery()" mat-list-item>
          <mat-icon>photo_library</mat-icon>
          <span>View Photos</span>
        </button>
        <button (click)="open()" *ngIf="electronService.isElectronApp" mat-list-item>
          <mat-icon>open_in_new</mat-icon>
          <span>Download Location</span>
        </button>
      </mat-action-list>
    </mat-card>
  `,
  styles: [
    `
      mat-card {
        padding: 0 0 8px 0;
      }
      mat-icon {
        margin-right: 5px;
      }
    `
  ],
  animations: [
    trigger('simpleFadeAnimation', [
      state('in', style({ opacity: 1 })),

      transition(':enter', [style({ opacity: 0 }), animate(300)])
    ])
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostContextMenuComponent {
  postState: PostState;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  fs;
  contextMenuService: ContextMenuService;
  constructor(
    public electronService: ElectronService,
    private dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone
  ) {
    if (this.electronService.isElectronApp) {
      this.fs = this.electronService.remote.require('fs');
    }
  }

  restart() {
    this.contextMenuService.closePostCtxtMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', [ this.postState.postId ]).subscribe(
      () => {
        this._snackBar.open('Download started', null, {
          duration: 5000
        });
      },
      error => {
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  remove() {
    this.contextMenuService.closePostCtxtMenu();
    this.ngZone.run(() => {
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
          this.httpClient.post<RemoveResponse>(this.serverService.baseUrl + '/post/remove', [ this.postState.postId ])
        )
      )
      .subscribe(
        () => {},
        error => {
          this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
    });
  }

  stop() {
    this.contextMenuService.closePostCtxtMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', [ this.postState.postId ]).subscribe(
      () => {
        this._snackBar.open('Download stopped', null, {
          duration: 5000
        });
      },
      error => {
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  seeDetails() {
    this.contextMenuService.closePostCtxtMenu();
    this.ngZone.run(() => {
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
    });
  }

  openGallery() {
    this.contextMenuService.closePostCtxtMenu();
    this.ngZone.run(() => {
      const dialogRef = this.dialog.open(GalleryComponent, {
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
    });
  }

  open() {
    this.contextMenuService.closePostCtxtMenu();
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
        this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }
}
