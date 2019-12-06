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
  constructor(
    private wsConnectionService: WsConnectionService,
    private ngZone: NgZone,
    private selectionService: SelectionService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  downloadSpeed: EventEmitter<DownloadSpeed> = new EventEmitter();
  subscriptions: Subscription[] = [];
  globalState: EventEmitter<GlobalState> = new EventEmitter();
  selected: EventEmitter<number> = new EventEmitter();

  ngAfterViewInit(): void {
    this.ngZone.run(() => {
      this.selected.emit(0);
      this.globalState.emit(new GlobalState(0, 0, 0, 0));
      this.downloadSpeed.emit(new DownloadSpeed('0 B'));
    });
  }

  ngOnInit() {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to global state and download speed');
      this.subscriptions.push(
        handler.subscribeForGlobalState((e: GlobalState[]) => {
          this.ngZone.run(() => {
            this.globalState.emit(e[0]);
          });
        })
      );
      this.subscriptions.push(
        handler.subscribeForSpeed((e: DownloadSpeed[]) => {
          this.ngZone.run(() => {
            this.downloadSpeed.emit(e[0]);
          });
        })
      );
      handler.send(new WSMessage(CMD.GLOBAL_STATE_SUB.toString()));
      handler.send(new WSMessage(CMD.SPEED_SUB.toString()));
    });
    this.subscriptions.push(
      this.selectionService.selected$.subscribe(selected => this.ngZone.run(() => this.selected.emit(selected.length)))
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.GLOBAL_STATE_UNSUB.toString()));
      handler.send(new WSMessage(CMD.SPEED_UNSUB.toString()));
    });
  }
}
