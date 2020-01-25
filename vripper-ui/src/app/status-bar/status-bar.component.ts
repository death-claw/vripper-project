import { flatMap, filter } from 'rxjs/operators';
import { SelectionService } from './../selection-service';
import {
  Component,
  OnInit,
  NgZone,
  OnDestroy,
  ChangeDetectionStrategy,
  EventEmitter,
  AfterViewInit
} from '@angular/core';
import { DownloadSpeed } from '../common/download-speed.model';
import { WsConnectionService } from '../ws-connection.service';
import { WsHandler } from '../ws-handler';
import { Subscription } from 'rxjs';
import { GlobalState } from '../common/global-state.model';
import { WSMessage } from '../common/ws-message.model';
import { CMD } from '../common/cmd.enum';

@Component({
  selector: 'app-status-bar',
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBarComponent implements OnInit, OnDestroy, AfterViewInit {
  constructor(private ws: WsConnectionService, private ngZone: NgZone, private selectionService: SelectionService) {}

  websocketHandlerPromise: Promise<WsHandler>;
  downloadSpeed: EventEmitter<DownloadSpeed> = new EventEmitter();
  subscriptions: Subscription[] = [];
  globalState: EventEmitter<GlobalState> = new EventEmitter();
  selected: EventEmitter<number> = new EventEmitter();

  globalStateSub: Subscription;
  speedSub: Subscription;

  ngAfterViewInit(): void {
    this.ngZone.run(() => {
      this.selected.emit(0);
      this.globalState.emit(new GlobalState(0, 0, 0, 0));
      this.downloadSpeed.emit(new DownloadSpeed('0 B'));
    });
  }

  ngOnInit() {
    console.log('Connecting to global state and download speed');
    this.subscriptions.push(
      this.ws.state.subscribe(state => {
        if (state) {
          this.globalStateSub = this.ws.subscribeForGlobalState().subscribe((e: GlobalState[]) => {
            this.ngZone.run(() => {
              this.globalState.emit(e[0]);
            });
          });

          this.speedSub = this.ws.subscribeForSpeed().subscribe((e: DownloadSpeed[]) => {
            this.ngZone.run(() => {
              this.downloadSpeed.emit(e[0]);
            });
          });
          this.ws.send(new WSMessage(CMD.GLOBAL_STATE_SUB.toString()));
          this.ws.send(new WSMessage(CMD.SPEED_SUB.toString()));
        } else {
          if (this.globalStateSub != null) {
            this.globalStateSub.unsubscribe();
          }
          if (this.speedSub != null) {
            this.speedSub.unsubscribe();
          }
        }
      })
    );

    this.subscriptions.push(
      this.selectionService.selected$.subscribe(selected => this.ngZone.run(() => this.selected.emit(selected.length)))
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
    this.ws.send(new WSMessage(CMD.GLOBAL_STATE_UNSUB.toString()));
    this.ws.send(new WSMessage(CMD.SPEED_UNSUB.toString()));
    if (this.globalStateSub != null) {
      this.globalStateSub.unsubscribe();
    }
    if (this.speedSub != null) {
      this.speedSub.unsubscribe();
    }
  }
}
