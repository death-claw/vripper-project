import {GrabQueueState} from '../domain/grab-queue.model';
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

  connect() {
    this.subscriptions.push(this.ws.queued$.subscribe((e: GrabQueueState[]) => {
      this.zone.run(() => {
        const toAdd = [];
        const toUpdate = [];
        e.forEach(v => {
          const rowNode: RowNode = this.gridOptions.api.getRowNode(v.threadId);
          if (rowNode == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridOptions.api.applyTransaction({update: toUpdate, add: toAdd});
        const count = this.gridOptions.api.getDisplayedRowCount();
        if (count > 0 && toAdd.length > 0) {
          this.notificationService.notifyFromGrabQueue(
            'Link Collector',
            `You have ${count} ${count > 1 ? 'threads' : 'thread'} waiting in the link collector`
          );
        }
      });
    }));

    this.subscriptions.push(this.ws.queuedRemove$.subscribe((e: string[]) => {
      this.zone.run(() => {
        const toRemove = [];
        e.forEach(v => {
          const rowNode: RowNode = this.gridOptions.api.getRowNode(v);
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
          return;
        });
        this.gridOptions.api.applyTransaction({remove: toRemove});
      });
    }));
  }

  disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
