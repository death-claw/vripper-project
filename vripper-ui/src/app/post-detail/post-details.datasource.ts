import { WsHandler } from './../ws-handler';
import { GridOptions } from 'ag-grid-community';
import { Subscription } from 'rxjs';
import { WsConnectionService, WSState } from '../ws-connection.service';
import { WSMessage } from '../common/ws-message.model';
import { CMD } from '../common/cmd.enum';
import { NgZone } from '@angular/core';

export class PostDetailsDataSource {
  constructor(
    private wsConnectionService: WsConnectionService,
    private gridOptions: GridOptions,
    private postId: string,
    private zone: NgZone
    ) {
    this.websocketHandlerPromise = wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;

  subscriptions: Subscription[] = [];

  connect() {
    console.log('Connecting to post details datasource');
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      this.subscriptions.push(
        handler.subscribeForPostDetails(e => {
          this.zone.run(() => {
            const toAdd = [];
            const toUpdate = [];
            e.forEach(v => {
              if (this.gridOptions.api.getRowNode(v.url) == null) {
                toAdd.push(v);
              } else {
                toUpdate.push(v);
              }
            });
            this.gridOptions.api.updateRowData({ update: toUpdate, add: toAdd });
          });
        })
      );
    });

    this.subscriptions.push(
      this.wsConnectionService.state.subscribe(e => {
        if (e === WSState.OPEN) {
          this.websocketHandlerPromise.then((handler: WsHandler) => {
            handler.send(new WSMessage(CMD.POST_DETAILS_SUB.toString(), this.postId));
          });
        }
      })
    );
  }

  disconnect() {
    console.log('Disconnecting from post details datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.POST_DETAILS_UNSUB.toString()));
    });
  }
}
