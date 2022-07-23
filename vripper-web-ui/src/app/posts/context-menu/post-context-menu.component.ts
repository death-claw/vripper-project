import {AppService} from '../../services/app.service';
import {ServerService} from '../../services/server-service';
import {HttpClient} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, NgZone} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {PhotosComponent} from '../../photos/photos.component';
import {Observable} from 'rxjs';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {Post} from '../../domain/post-state.model';
import {ConfirmDialogComponent} from '../../confirmation-component/confirmation-dialog';
import {filter, flatMap} from 'rxjs/operators';
import {RemoveResponse} from '../../domain/remove-response.model';
import {PostContextMenuService} from '../../services/post-context-menu.service';
import {PostsService} from '../../services/posts.service';
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
  post!: Post;
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  contextMenuService!: PostContextMenuService;

  constructor(
    private dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone,
    private appService: AppService,
    private postsDataService: PostsService
  ) {
  }


  restart() {
    this.contextMenuService.closePostContextMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', [this.post.postId]).subscribe(
      {
        error: error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      }
    );
  }

  goTo() {
    this.contextMenuService.closePostContextMenu();
    window.open(this.post.url, '_blank');
  }

  remove() {
    this.contextMenuService.closePostContextMenu();
    this.ngZone.run(() => {
      this.dialog
        .open(ConfirmDialogComponent, {
          maxHeight: '100vh',
          maxWidth: '100vw',
          height: '200px',
          width: '400px',
          data: {header: 'Confirmation', content: 'Are you sure you want to remove this item ?'}
        })
        .afterClosed()
        .pipe(
          filter(e => e === 'yes'),
          flatMap(() =>
            this.httpClient.post<string[]>(this.serverService.baseUrl + '/post/remove', [this.post.postId])
          )
        )
        .subscribe({
            next: data => {
              this.postsDataService.remove(data);
            }, error: error => {
              this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
                duration: 5000
              });
            }
          }
        );
    });
  }

  stop() {
    this.contextMenuService.closePostContextMenu();
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', [this.post.postId]).subscribe({
        error: error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
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
}
