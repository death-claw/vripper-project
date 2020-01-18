import { GrabQueueState } from './grab-queue.model';
import { CMD } from './../common/cmd.enum';
import { WSMessage } from './../common/ws-message.model';
import { Subscription } from 'rxjs';
import { WsConnectionService } from '../ws-connection.service';
import { NotificationService } from '../notification.service';
import { GridOptions } from 'ag-grid-community';
import { WsHandler } from '../ws-handler';
import { NgZone } from '@angular/core';

export class GrabQueueDataSource {
  constructor(
    private wsConnectionService: WsConnectionService,
    private gridOptions: GridOptions,
    private zone: NgZone,
    private notificationService: NotificationService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscriptions: Subscription[] = [];

  connect() {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to grab queue datasource');
      this.subscriptions.push(
        handler.subscribeForGrabQueue((e: GrabQueueState[]) => {
          this.zone.run(() => {
            const toAdd = [];
            const toUpdate = [];
            const toRemove = [];
            e.forEach(v => {
              if (v.removed) {
                if (this.gridOptions.api.getRowNode(v.link) != null) {
                  toRemove.push(this.gridOptions.api.getRowNode(v.link).data);
                }
                return;
              }

              if (this.gridOptions.api.getRowNode(v.link) == null) {
                toAdd.push(v);
              } else {
                toUpdate.push(v);
              }
            });
            this.gridOptions.api.updateRowData({ update: toUpdate, add: toAdd, remove: toRemove });
            const count = this.gridOptions.api.getDisplayedRowCount();
            if (count > 0 && toAdd.length > 0) {
              this.notificationService.notifyFromGrabQueue(
                'Link Collector',
                `${count} ${count > 1 ? 'threads are' : 'thread is'} in the link collector`
              );
            }
          });
        })
      );
      handler.send(new WSMessage(CMD.GRAB_QUEUE_SUB.toString()));
    });
  }

  disconnect() {
    console.log('Disconnecting from grab queue datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.GRAB_QUEUE_UNSUB.toString()));
    });
  }
}
