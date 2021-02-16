import {MultiPostModel} from '../domain/multi-post.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {GridOptions, RowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class MultiPostGridDataSource {
  subscriptions: Subscription[] = [];

  constructor(
    private ws: WsConnectionService,
    private gridOptions: GridOptions,
    private zone: NgZone
  ) {
  }

  connect() {
    this.subscriptions.push(this.ws.multiPosts$.subscribe((e: MultiPostModel[]) => {
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
    console.log('Disconnecting from link collector datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
