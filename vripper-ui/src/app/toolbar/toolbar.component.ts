import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {RemoveAllResponse} from '../domain/remove-all-response.model';
import {ServerService} from '../services/server-service';
import {AppService} from '../services/app.service';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ConfirmDialogComponent} from '../confirmation-component/confirmation-dialog';
import {filter, flatMap} from 'rxjs/operators';
import {LoggedUser} from '../domain/logged-user.model';
import {SettingsComponent} from '../settings/settings.component';
import {BehaviorSubject, Observable, Subject, Subscription} from 'rxjs';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {WsConnectionService} from '../services/ws-connection.service';
import {SelectionService} from '../services/selection-service';
import {RowNode} from 'ag-grid-community';
import {RemoveResponse} from '../domain/remove-response.model';
import {PostId} from '../domain/post-id.model';
import {PostsService} from '../services/posts.service';

@Component({
  selector: 'app-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToolbarComponent implements OnInit, OnDestroy {
  user$: Subject<LoggedUser> = new BehaviorSubject({user: ''});
  disableSelection$: Subject<boolean> = new BehaviorSubject(true);
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  selected: RowNode[] = [];
  subscriptions: Subscription[] = [];

  constructor(
    private serverService: ServerService,
    private appService: AppService,
    private ngZone: NgZone,
    private httpClient: HttpClient,
    private _snackBar: MatSnackBar,
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private ws: WsConnectionService,
    private selectionService: SelectionService,
    private postsDataService: PostsService
  ) {
  }

  openSettings(): void {
    const dialogRef = this.dialog.open(SettingsComponent, {
      width: '70%',
      height: '70%',
      maxWidth: '100vw',
      maxHeight: '100vh'
    });

    const smallDialogSubscription = this.isExtraSmall.subscribe(result => {
      if (result.matches) {
        dialogRef.updateSize('100%', '100%');
      } else {
        dialogRef.updateSize('70%', '70%');
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      smallDialogSubscription.unsubscribe();
    });
  }

  scan() {
    this.appService.scan();
  }

  search(event) {
    this.postsDataService.search(event);
  }

  remove() {
    const toRemove = [];
    this.selected.forEach(e => toRemove.push(e.data.postId));
    this.dialog
      .open(ConfirmDialogComponent, {
        maxHeight: '100vh',
        maxWidth: '100vw',
        height: '200px',
        width: '60%',
        data: {header: 'Confirmation', content: 'Are you sure you want to remove the selected items ?'}
      })
      .afterClosed()
      .pipe(
        filter(e => e === 'yes'),
        flatMap(e => this.httpClient.post<RemoveResponse[]>(this.serverService.baseUrl + '/post/remove', toRemove))
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
  }

  restart() {
    const toStart = [];
    this.selected.forEach(e => toStart.push(e.data.postId));
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', toStart).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  stop() {
    const toStop = [];
    this.selected.forEach(e => toStop.push(e.data.postId));
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', toStop).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  rename() {
    const toRename = [];
    this.selected.forEach(e => toRename.push(e.data.postId));
    this.httpClient.post<PostId[]>(this.serverService.baseUrl + '/post/rename/first', toRename).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
          duration: 5000
        });
      }
    );
  }

  clear() {
    this.ngZone.run(() => {
      this.httpClient.post<RemoveAllResponse>(this.serverService.baseUrl + '/post/clear/all', {}).subscribe(
        data => {
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
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
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
    });
  }

  ngOnInit() {
    this.subscriptions.push(this.ws.user$.subscribe(e => this.ngZone.run(() => this.user$.next(e))));
    this.subscriptions.push(
      this.selectionService.selected$.subscribe(selected => {
        this.selected = selected;
        this.ngZone.run(() => this.disableSelection$.next(this.selected.length === 0));
      })
    );
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
