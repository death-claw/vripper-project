import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {ServerService} from '../services/server-service';
import {AppService} from '../services/app.service';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ConfirmDialogComponent} from '../confirmation-component/confirmation-dialog';
import {filter, flatMap} from 'rxjs/operators';
import {SettingsComponent} from '../settings/settings.component';
import {BehaviorSubject, Observable, Subject, Subscription} from 'rxjs';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {WsConnectionService} from '../services/ws-connection.service';
import {SelectionService} from '../services/selection-service';
import {IRowNode} from 'ag-grid-community';
import {PostsService} from '../services/posts.service';
import {HomeTabsService} from '../services/home-tabs.service';
import {LinkCollectorService} from '../services/link-collector.service';
import {EventLogService} from '../services/event-log.service';
import {AboutComponent} from '../about/about-component';

@Component({
  selector: 'app-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToolbarComponent implements OnInit, OnDestroy {
  user$: Subject<string> = new BehaviorSubject('');
  disableSelection$: Subject<boolean> = new BehaviorSubject(true);
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);
  selected: IRowNode[] = [];
  subscriptions: Subscription[] = [];
  tabIndex!: number;
  searchModel!: string;

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
    private postsDataService: PostsService,
    public homeTabsService: HomeTabsService,
    public linkCollectorService: LinkCollectorService,
    public eventLogService: EventLogService,
  ) {
    this.homeTabsService.index.subscribe(e => {
      this.tabIndex = e;
      this.search(this.searchModel);
    });
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

  search(event: any) {
    switch (this.tabIndex) {
      case 0:
        this.postsDataService.search(event);
        break;
      case 1:
        this.linkCollectorService.search(event);
        break;
      case 2:
        this.eventLogService.search(event);
        break;
    }
  }

  remove() {
    const toRemove: any[] = [];
    this.selected.forEach(e => toRemove.push(e.data.postId));
    this.dialog
      .open(ConfirmDialogComponent, {
        maxHeight: '100vh',
        maxWidth: '100vw',
        height: '200px',
        width: '400px',
        data: {header: 'Confirmation', content: 'Are you sure you want to remove the selected items ?'}
      })
      .afterClosed()
      .pipe(
        filter(e => e === 'yes'),
        flatMap(e => this.httpClient.post<string[]>(this.serverService.baseUrl + '/post/remove', toRemove))
      )
      .subscribe(
        data => {
          this.postsDataService.remove(data);
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      );
  }

  restart() {
    const toStart: any[] = [];
    this.selected.forEach(e => toStart.push(e.data.postId));
    this.httpClient.post(this.serverService.baseUrl + '/post/restart', toStart).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
          duration: 5000
        });
      }
    );
  }

  stop() {
    const toStop: any[] = [];
    this.selected.forEach(e => toStop.push(e.data.postId));
    this.httpClient.post(this.serverService.baseUrl + '/post/stop', toStop).subscribe(
      () => {
      },
      error => {
        this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
          duration: 5000
        });
      }
    );
  }

  clear() {
    this.ngZone.run(() => {
      this.httpClient.post(this.serverService.baseUrl + '/post/clear/all', {}).subscribe(
        data => {
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      );
    });
  }

  clearLinkCollector() {
    this.ngZone.run(() => {
      this.httpClient.get<void>(this.serverService.baseUrl + '/grab/clear', {}).subscribe(
        () => {
          this.linkCollectorService.clear();
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      );
    });
  }

  clearEventLogs() {
    this.ngZone.run(() => {
      this.httpClient.get<void>(this.serverService.baseUrl + '/events/clear', {}).subscribe(
        () => {
          this.eventLogService.clear();
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
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
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', undefined, {
            duration: 5000
          });
        }
      );
    });
  }

  ngOnInit() {
    this.subscriptions.push(this.ws.globalState$.subscribe(e => this.ngZone.run(() => this.user$.next(e.loggedUser))));
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

  openAbout(): void {
    const dialogRef = this.dialog.open(AboutComponent, {
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
}
