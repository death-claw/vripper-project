import { AppService } from './app.service';
import { ClipboardService } from './clipboard.service';
import { ElectronService } from 'ngx-electron';
import { Component, OnDestroy, AfterViewInit, Renderer2, ChangeDetectionStrategy, NgZone } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Subscription, BehaviorSubject, Subject, merge } from 'rxjs';
import { WsConnectionService } from './ws-connection.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnDestroy, AfterViewInit {
  constructor(
    private dialog: MatDialog,
    public ws: WsConnectionService,
    public electronService: ElectronService,
    private clipboardService: ClipboardService,
    private appService: AppService,
    private renderer: Renderer2,
    private ngZone: NgZone
  ) {
    this.electron = new BehaviorSubject(electronService.isElectronApp);
  }

  private subscriptions: Subscription[] = [];
  electron: Subject<boolean>;

  loaded: Subject<boolean> = new BehaviorSubject(false);

  ngAfterViewInit() {
    this.appService.renderer = this.renderer;
    this.subscriptions.push(
      this.ws.state.subscribe(online => {
        if (!online) {
          this.ngZone.run(() => this.loaded.next(false));
          setTimeout(
            () =>
              this.ngZone.run(() => {
                this.dialog.closeAll();
              }),
            500
          );
        } else {
          this.ngZone.run(() => this.loaded.next(true));
          this.clipboardService.init();
          this.subscriptions.push(merge(this.appService.loadTheme(), this.appService.loadSettings()).subscribe());
        }
      })
    );
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
