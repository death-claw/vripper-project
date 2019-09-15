import { Component, OnInit, NgZone, OnDestroy } from '@angular/core';
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
  styleUrls: ['./status-bar.component.scss']
})
export class StatusBarComponent implements OnInit, OnDestroy {

  constructor(private wsConnectionService: WsConnectionService, private ngZone: NgZone) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  downloadSpeed: DownloadSpeed = new DownloadSpeed('0 B');
  subscriptions: Subscription[] = [];
  globalState: GlobalState = new GlobalState(0, 0, 0, 0);

  ngOnInit() {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to global state and download speed');
      this.subscriptions.push(
        handler.subscribeForGlobalState((e: GlobalState[]) => {
          this.ngZone.run(() => {
            this.globalState = e[0];
          });
        })
      );
      this.subscriptions.push(
        handler.subscribeForSpeed((e: DownloadSpeed[]) => {
          this.ngZone.run(() => {
            this.downloadSpeed = e[0];
          });
        })
      );
      handler.send(new WSMessage(CMD.GLOBAL_STATE_SUB.toString()));
      handler.send(new WSMessage(CMD.SPEED_SUB.toString()));
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.GLOBAL_STATE_UNSUB.toString()));
      handler.send(new WSMessage(CMD.SPEED_UNSUB.toString()));
    });
  }
}
