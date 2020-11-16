import {GridOptions} from 'ag-grid-community';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {NgZone} from '@angular/core';

export class PhotosDatasource {
  constructor(
    private ws: WsConnectionService,
    private gridOptions: GridOptions,
    private postId: string,
    private zone: NgZone
  ) {
  }

  subscriptions: Subscription[] = [];
  postDetailsSub: Subscription;

  connect() {
    this.postDetailsSub = this.ws.postDetails$(this.postId).subscribe(e => {
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
        this.gridOptions.api.applyTransaction({update: toUpdate, add: toAdd});
      });
    });
  }

  disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
    if (this.postDetailsSub != null) {
      this.postDetailsSub.unsubscribe();
    }
  }
}
