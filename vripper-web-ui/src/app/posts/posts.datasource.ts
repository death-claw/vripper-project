import {Post} from '../domain/post-state.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {GridApi, IRowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class PostsDataSource {
  subscriptions: Subscription[] = [];

  constructor(private ws: WsConnectionService, private gridApi: GridApi, private zone: NgZone) {
  }

  connect() {
    this.subscriptions.push(this.ws.posts$.subscribe((e: Post[]) => {
      this.zone.run(() => {
        const toAdd: Post[] = [];
        const toUpdate: Post[] = [];
        e.forEach(v => {
          if (this.gridApi.getRowNode(v.postId) == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridApi.applyTransaction({update: toUpdate, add: toAdd});
      });
    }));

    this.subscriptions.push(this.ws.postsRemove$.subscribe((e: string[]) => {
      this.zone.run(() => {
        const toRemove: any[] = [];
        e.forEach(v => {
          const rowNode: IRowNode | undefined = this.gridApi.getRowNode(v);
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
          return;
        });
        this.gridApi.applyTransaction({remove: toRemove});
      });
    }));
  }

  disconnect() {
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
