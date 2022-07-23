import {GridApi} from 'ag-grid-community';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../services/ws-connection.service';
import {NgZone} from '@angular/core';
import {Photo} from '../domain/photo-model';

export class PhotosDatasource {
  subscriptions: Subscription[] = [];
  postDetailsSub!: Subscription;

  constructor(
    private ws: WsConnectionService,
    private gridApi: GridApi,
    private postId: string,
    private zone: NgZone
  ) {
  }

  connect() {
    this.postDetailsSub = this.ws.postDetails$(this.postId).subscribe(e => {
      this.zone.run(() => {
        const toAdd: Photo[] = [];
        const toUpdate: Photo[] = [];
        e.forEach(v => {
          if (this.gridApi.getRowNode(v.url) == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridApi.applyTransaction({update: toUpdate, add: toAdd});
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
