import {AppService} from './services/app.service';
import {AfterViewInit, ChangeDetectionStrategy, Component, NgZone, OnDestroy, Renderer2} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {BehaviorSubject, merge, Subject, Subscription} from 'rxjs';
import {WsConnectionService} from './services/ws-connection.service';
import {RxStompState} from '@stomp/rx-stomp';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnDestroy, AfterViewInit {
  loaded: Subject<boolean> = new BehaviorSubject(false);
  private subscriptions: Subscription[] = [];

  constructor(
    private dialog: MatDialog,
    public ws: WsConnectionService,
    private appService: AppService,
    private renderer: Renderer2,
    private ngZone: NgZone
  ) {}

  ngAfterViewInit() {
    this.appService.renderer = this.renderer;
    this.subscriptions.push(
      this.ws.state.subscribe(state => {
        if (state === RxStompState.OPEN) {
          this.ngZone.run(() => this.loaded.next(true));
          this.subscriptions.push(merge(this.appService.loadTheme(), this.appService.loadSettings()).subscribe());
        } else {
          this.ngZone.run(() => this.loaded.next(false));
          setTimeout(
            () =>
              this.ngZone.run(() => {
                this.dialog.closeAll();
              }),
            500
          );
        }
      })
    );
  }

  ngOnDestroy() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
