import { ClipboardService } from './clipboard.service';
import { ElectronService } from 'ngx-electron';
import { SettingsComponent } from './settings/settings.component';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { MatDialog } from '@angular/material';
import { BreakpointObserver, BreakpointState, Breakpoints } from '@angular/cdk/layout';
import { Observable, Subscription } from 'rxjs';
import { WsConnectionService, WSState } from './ws-connection.service';
import { LoggedUser } from './common/logged-user.model';
import { WsHandler } from './ws-handler';
import { CMD } from './common/cmd.enum';
import { WSMessage } from './common/ws-message.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  constructor(
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private ws: WsConnectionService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private ngZone: NgZone,
  ) {
    this.websocketHandlerPromise = this.ws.getConnection();
  }

  subscriptions: Subscription[] = [];
  websocketHandlerPromise: Promise<WsHandler>;
  currentState: WSState;

  loggedUser: LoggedUser = new LoggedUser(null);

  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

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
    return this.currentState === WSState.INIT || this.currentState === WSState.CONNECTING;
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
      }
    });
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => {
      e.unsubscribe();
    });
  }
}
