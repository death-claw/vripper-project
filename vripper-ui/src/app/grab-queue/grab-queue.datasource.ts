import {GrabQueueState} from './grab-queue.model';
import {CMD} from './../common/cmd.enum';
import {WSMessage} from './../common/ws-message.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../ws-connection.service';
import {NotificationService} from '../notification.service';
import {GridOptions, RowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class GrabQueueDataSource {
  constructor(
    private ws: WsConnectionService,
    private gridOptions: GridOptions,
    private zone: NgZone,
    private notificationService: NotificationService
  ) {
  }

  subscriptions: Subscription[] = [];
  grabQueueSub: Subscription;

  connect() {
    console.log('Connecting to grab queue datasource');
    this.subscriptions.push(
      this.ws.state.subscribe(state => {
        if (state) {
          this.grabQueueSub = this.ws.subscribeForGrabQueue().subscribe((e: GrabQueueState[]) => {
            this.zone.run(() => {
              const toAdd = [];
              const toUpdate = [];
              const toRemove = [];
              e.forEach(v => {
                const rowNode: RowNode = this.gridOptions.api.getRowNode(v.threadId);
                if (v.removed) {
                  if (rowNode != null) {
                    toRemove.push(rowNode.data);
                  }
                  return;
                }
                if (rowNode == null) {
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
                  `You have ${count} ${count > 1 ? 'threads' : 'thread'} waiting in the link collector`
                );
              }
            });
          });
          this.ws.send(new WSMessage(CMD.GRAB_QUEUE_SUB.toString()));
        } else if (this.grabQueueSub != null) {
          this.grabQueueSub.unsubscribe();
        }
      })
    );
  }

  disconnect() {
    console.log('Disconnecting from grab queue datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.ws.send(new WSMessage(CMD.GRAB_QUEUE_UNSUB.toString()));
    if (this.grabQueueSub != null) {
      this.grabQueueSub.unsubscribe();
    }
  }
}
