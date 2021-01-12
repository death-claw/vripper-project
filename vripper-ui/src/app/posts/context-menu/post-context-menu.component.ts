import {AppService} from '../../services/app.service';
import {ServerService} from '../../services/server-service';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {ElectronService} from 'ngx-electron';
import {ChangeDetectionStrategy, Component, NgZone} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {PhotosComponent} from '../../photos/photos.component';
import {Observable} from 'rxjs';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {Post} from '../../domain/post-state.model';
import {DownloadPath} from '../../domain/download-path.model';
import {ConfirmDialogComponent} from '../../confirmation-component/confirmation-dialog';
import {filter, flatMap} from 'rxjs/operators';
import {RemoveResponse} from '../../domain/remove-response.model';
import {PostContextMenuService} from '../../services/post-context-menu.service';
import {PostsService} from '../../services/posts.service';
import {AlternativeTitleComponent, AlternativeTitleDialog} from '../alternative-title/alternative-title.component';
import {PostId} from '../../domain/post-id.model';

@Component({
  selector: 'app-post-ctx-menu',
  templateUrl: 'post-context-menu.component.html',
  styleUrls: ['post-context-menu.component.scss'],
  animations: [
    trigger('simpleFadeAnimation', [
      state('in', style({opacity: 1})),

      transition(':enter', [style({opacity: 0}), animate(300)])
    ])
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostContextMenuComponent {
  post: Post;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  fs;
  contextMenuService: PostContextMenuService;

  constructor(
    public electronService: ElectronService,
    private dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone,
    private appService: AppService,
    private postsDataService: PostsService
  ) {
    if (this.electronService.isElectronApp) {
      this.fs = this.electronService.remote.require('fs');
    }
  }


  restart() {
    this.contextMenuService.closePostContextMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', [this.post.postId]).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  goTo() {
    this.contextMenuService.closePostContextMenu();
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.post.url);
    } else {
      window.open(this.post.url, '_blank');
    }
  }

  remove() {
    this.contextMenuService.closePostContextMenu();
    this.ngZone.run(() => {
      this.dialog
        .open(ConfirmDialogComponent, {
          maxHeight: '100vh',
          maxWidth: '100vw',
          height: '200px',
          width: '60%',
          data: {header: 'Confirmation', content: 'Are you sure you want to remove this item ?'}
        })
        .afterClosed()
        .pipe(
          filter(e => e === 'yes'),
          flatMap(() =>
            this.httpClient.post<RemoveResponse[]>(this.serverService.baseUrl + '/post/remove', [this.post.postId])
          )
        )
        .subscribe(
          data => {
            this.postsDataService.remove(data);
          },
          error => {
            this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
              duration: 5000
            });
          }
        );
    });
  }

  useAltTitle() {
    this.contextMenuService.closePostContextMenu();
    this.httpClient.post<PostId[]>(this.serverService.baseUrl + '/post/rename/first', [{
      postId: this.post.postId
    }]).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  openRenameDialog() {
    this.contextMenuService.closePostContextMenu();
    const dialogConfig: MatDialogConfig<AlternativeTitleDialog> = {
      maxHeight: '100vh',
      maxWidth: '100vw',
      height: '300px',
      width: '60%',
      data: {post: this.post}
    };
    this.ngZone.run(() => {
      this.dialog
        .open(AlternativeTitleComponent, dialogConfig);
    });
  }

  stop() {
    this.contextMenuService.closePostContextMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', [this.post.postId]).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  seeDetails() {
    this.contextMenuService.closePostContextMenu();
    this.ngZone.run(() => {
      const dialogRef = this.dialog.open(PhotosComponent, {
        width: '90%',
        height: '90%',
        maxWidth: '100vw',
        maxHeight: '100vh',
        data: this.post
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
    this.contextMenuService.closePostContextMenu();
    if (!this.electronService.isElectronApp) {
      console.error('Cannot open downloader folder, not electron app');
      return;
    }
    // Request the server to give the correct file location
    this.httpClient.get<DownloadPath>(this.serverService.baseUrl + '/post/path/' + this.post.postId).subscribe(
      path => {
        if (this.fs.existsSync(path.path)) {
          this.electronService.shell.openPath(path.path).then();
        } else {
          this._snackBar.open(path.path + ' does not exist, you probably removed it', null, {
            duration: 5000
          });
        }
      },
      (error: HttpErrorResponse) => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }
}
