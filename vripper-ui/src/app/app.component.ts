import { AppService } from './app.service';
import { ClipboardService } from './clipboard.service';
import { ElectronService } from 'ngx-electron';
import { Component, OnInit, OnDestroy, NgZone, AfterViewInit, Renderer2 } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Subscription } from 'rxjs';
import { WsConnectionService, WSState } from './ws-connection.service';
import { WsHandler } from './ws-handler';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy, AfterViewInit {
  constructor(
    public dialog: MatDialog,
    private ws: WsConnectionService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private ngZone: NgZone,
    private appService: AppService,
    private renderer: Renderer2
  ) {}

  subscriptions: Subscription[] = [];
  websocketHandlerPromise: Promise<WsHandler>;
  currentState: WSState;
  themeLoaded = false;

  noConnectionState(): boolean {
    return this.currentState === WSState.CLOSE || this.currentState === WSState.ERROR;
  }

  connecting(): boolean {
    return (this.currentState === WSState.INIT || this.currentState === WSState.CONNECTING) && !this.themeLoaded ;
  }

  ngAfterViewInit() {
    this.appService.renderer = this.renderer;
  }

  ngOnInit() {
    this.currentState = WSState.INIT;
    this.subscriptions.push(this.ws.state.subscribe(e => {
      this.currentState = e;
      if (this.currentState === WSState.CLOSE || this.currentState === WSState.ERROR) {
        this.dialog.closeAll();
      } else if (this.currentState === WSState.OPEN) {
        this.clipboardService.init();
        this.appService
          .loadTheme()
          .subscribe(() => this.ngZone.run(() => this.themeLoaded = true));
      }
    }));
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => {
      e.unsubscribe();
    });
  }
}
