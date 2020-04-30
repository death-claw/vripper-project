import {GridOptions} from 'ag-grid-community';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../ws-connection.service';
import {WSMessage} from '../domain/ws-message.model';
import {CMD} from '../domain/cmd.enum';
import {NgZone} from '@angular/core';

export class PostDetailDatasource {
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
    console.log('Connecting to post details datasource');
    this.subscriptions.push(
      this.ws.state.subscribe(state => {
        if (state) {
          this.postDetailsSub = this.ws.subscribeForPostDetails().subscribe(e => {
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
          });
          this.ws.send(new WSMessage(CMD.POST_DETAILS_SUB.toString(), this.postId));
        } else if (this.postDetailsSub != null) {
          this.postDetailsSub.unsubscribe();
        }
      })
    );
  }

  disconnect() {
    console.log('Disconnecting from post details datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.ws.send(new WSMessage(CMD.POST_DETAILS_UNSUB.toString()));
    if (this.postDetailsSub != null) {
      this.postDetailsSub.unsubscribe();
    }
  }
}
