import { AppService } from './app.service';
import { ClipboardService } from './clipboard.service';
import { ElectronService } from 'ngx-electron';
import { Component, OnDestroy, AfterViewInit, Renderer2, ChangeDetectionStrategy, NgZone } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Subscription, BehaviorSubject, Subject, merge } from 'rxjs';
import { WsConnectionService, WSState } from './ws-connection.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnDestroy, AfterViewInit {
  constructor(
    private dialog: MatDialog,
    private ws: WsConnectionService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private appService: AppService,
    private renderer: Renderer2,
    private ngZone: NgZone
  ) {
    this.electron = new BehaviorSubject(electronService.isElectronApp);
  }

  private subscriptions: Subscription[] = [];
  appState: Subject<string> = new BehaviorSubject('CONNECTING');
  electron: Subject<boolean>;

  ngAfterViewInit() {
    this.appService.renderer = this.renderer;
    this.subscriptions.push(this.ws.state.subscribe(wsState => {
      if (wsState === WSState.CLOSE || wsState === WSState.ERROR) {
        setTimeout(() => this.ngZone.run(() => {
          this.dialog.closeAll();
          this.appState.next('DISCONNECTED');
        }), 500);
      } else if (wsState === WSState.OPEN) {
        this.clipboardService.init();
        merge(this.appService.loadTheme(), this.appService.loadSettings())
        .subscribe(() => this.ngZone.run(() => this.appState.next('CONNECTED')));
      }
    }));
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => {
      e.unsubscribe();
    });
  }
}
