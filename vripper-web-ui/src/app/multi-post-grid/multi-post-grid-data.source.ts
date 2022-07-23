import {MultiPostModel} from '../domain/multi-post.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {GridApi, IRowNode, RowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class MultiPostGridDataSource {
  subscriptions: Subscription[] = [];

  constructor(
    private ws: WsConnectionService,
    private gridApi: GridApi,
    private zone: NgZone
  ) {
  }

  connect() {
    this.subscriptions.push(this.ws.multiPosts$.subscribe((e: MultiPostModel[]) => {
      this.zone.run(() => {
        const toAdd: MultiPostModel[] = [];
        const toUpdate: MultiPostModel[] = [];
        e.forEach(v => {
          const rowNode: IRowNode<MultiPostModel> | undefined = this.gridApi.getRowNode(v.threadId);
          if (rowNode == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridApi.applyTransaction({update: toUpdate, add: toAdd});
      });
    }));

    this.subscriptions.push(this.ws.queuedRemove$.subscribe((e: string[]) => {
      this.zone.run(() => {
        const toRemove: MultiPostModel[] = [];
        e.forEach(v => {
          const rowNode: IRowNode<MultiPostModel> | undefined = this.gridApi.getRowNode(v);
          if (rowNode != null && rowNode.data != null) {
            toRemove.push(rowNode.data);
          }
          return;
        });
        this.gridApi.applyTransaction({remove: toRemove});
      });
    }));
  }

  disconnect() {
    console.log('Disconnecting from link collector datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
