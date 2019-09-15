import { AppService } from './app.service';
import { ClipboardService } from './clipboard.service';
import { ElectronService } from 'ngx-electron';
import { SettingsComponent } from './settings/settings.component';
import { Component, OnInit, OnDestroy, NgZone, Renderer2, AfterViewInit } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';
import { BreakpointObserver, BreakpointState, Breakpoints } from '@angular/cdk/layout';
import { Observable, Subscription } from 'rxjs';
import { WsConnectionService, WSState } from './ws-connection.service';
import { LoggedUser } from './common/logged-user.model';
import { WsHandler } from './ws-handler';
import { CMD } from './common/cmd.enum';
import { WSMessage } from './common/ws-message.model';
import { HttpClient } from '@angular/common/http';
import { RemoveAllResponse } from './common/remove-all-response.model';
import { ServerService } from './server-service';
import { ConfirmDialogComponent } from './common/confirmation-component/confirmation-dialog';
import { filter, flatMap } from 'rxjs/operators';
import { ScanComponent } from './scan/scan.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy, AfterViewInit {
  constructor(
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private ws: WsConnectionService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private ngZone: NgZone,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private renderer: Renderer2,
    private appService: AppService
  ) {
    this.websocketHandlerPromise = this.ws.getConnection();
  }

  subscriptions: Subscription[] = [];
  websocketHandlerPromise: Promise<WsHandler>;
  currentState: WSState;
  themeLoaded = false;
  loggedUser: LoggedUser = new LoggedUser(null);
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

  ngAfterViewInit() {
    this.appService.renderer = this.renderer;
  }

  scan() {
    const dialogRef = this.dialog.open(ScanComponent, {
      width: '70%',
      height: '70%',
      maxWidth: '100vw',
      maxHeight: '100vh'
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

  noConnectionState(): boolean {
    return this.currentState === WSState.CLOSE || this.currentState === WSState.ERROR;
  }

  connecting(): boolean {
    return (this.currentState === WSState.INIT || this.currentState === WSState.CONNECTING) && !this.themeLoaded ;
  }

  ngOnInit() {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to user stream');
      this.subscriptions.push(
        handler.subscribeForUser((e: LoggedUser[]) => {
          this.ngZone.run(() => {
            this.loggedUser = e[0];
          });
        })
      );
      handler.send(new WSMessage(CMD.USER_SUB.toString()));
    });
    this.currentState = WSState.INIT;
    this.ws.state.subscribe(e => {
      this.currentState = e;
      if (this.currentState === WSState.CLOSE || this.currentState === WSState.ERROR) {
        this.dialog.closeAll();
      } else if (this.currentState === WSState.OPEN) {
        this.clipboardService.init();
        this.appService
          .loadTheme()
          .subscribe(() => this.ngZone.run(() => this.themeLoaded = true));
      }
    });
  }

  clear() {
    this.ngZone.run(() => {
      this.httpClient.post<RemoveAllResponse>(this.serverService.baseUrl + '/post/clear/all', {}).subscribe(
        data => {
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
    this.subscriptions.forEach(e => {
      e.unsubscribe();
    });
  }
}
