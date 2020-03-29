import {PostState} from './post-state.model';
import {CMD} from '../common/cmd.enum';
import {WSMessage} from '../common/ws-message.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../ws-connection.service';
import {GridOptions} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class PostsDataSource {
  constructor(private ws: WsConnectionService, private gridOptions: GridOptions, private zone: NgZone) {
  }

  subscriptions: Subscription[] = [];

  postsSub: Subscription;

  connect() {
    console.log('Connecting to posts datasource');
    this.subscriptions.push(
      this.ws.state.subscribe(state => {
        if (state) {
          this.postsSub = this.ws.subscribeForPosts().subscribe((e: PostState[]) => {
            this.zone.run(() => {
              const toAdd = [];
              const toUpdate = [];
              const toRemove = [];
              e.forEach(v => {
                if (v.removed) {
                  if (this.gridOptions.api.getRowNode(v.postId) != null) {
                    toRemove.push(this.gridOptions.api.getRowNode(v.postId).data);
                  }
                  return;
                }
                if (this.gridOptions.api.getRowNode(v.postId) == null) {
                  toAdd.push(v);
                } else {
                  toUpdate.push(v);
                }
              });
              this.gridOptions.api.updateRowData({ update: toUpdate, add: toAdd, remove: toRemove });
            });
          });
          this.ws.send(new WSMessage(CMD.POSTS_SUB.toString()));
        } else if (this.postsSub != null) {
          this.postsSub.unsubscribe();
        }
      })
    );
  }

  disconnect() {
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.ws.send(new WSMessage(CMD.POSTS_UNSUB.toString()));
    if (this.postsSub != null) {
      this.postsSub.unsubscribe();
    }
  }
}
