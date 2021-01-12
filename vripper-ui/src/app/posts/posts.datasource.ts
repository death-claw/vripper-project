import {Post} from '../domain/post-state.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {GridOptions, RowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class PostsDataSource {
  subscriptions: Subscription[] = [];

  constructor(private ws: WsConnectionService, private gridOptions: GridOptions, private zone: NgZone) {
  }

  connect() {
    this.subscriptions.push(this.ws.posts$.subscribe((e: Post[]) => {
      this.zone.run(() => {
        const toAdd = [];
        const toUpdate = [];
        e.forEach(v => {
          if (this.gridOptions.api.getRowNode(v.postId) == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridOptions.api.applyTransaction({update: toUpdate, add: toAdd});
      });
    }));

    this.subscriptions.push(this.ws.postsRemove$.subscribe((e: string[]) => {
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
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
