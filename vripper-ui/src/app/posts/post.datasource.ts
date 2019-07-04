import { Action } from './../common/action.model';
import { PostState } from './post-state.model';
import { CMD } from './../common/cmd.enum';
import { WSMessage } from './../common/ws-message.model';
import { Subscription } from 'rxjs';
import { WsConnectionService } from '../ws-connection.service';
import { GridOptions } from 'ag-grid-community';
import { WsHandler } from '../ws-handler';
import { NgZone } from '@angular/core';

export class PostsDataSource {
  constructor(
    private wsConnectionService: WsConnectionService,
    private gridOptions: GridOptions,
    private zone: NgZone
  ) {
    this.websocketHandlerPromise = wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscriptions: Subscription[] = [];

  connect() {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      console.log('Connecting to posts datasource');
      this.subscriptions.push(
        handler.subscribeForPosts((e: PostState[], a: Action[]) => {
          this.zone.run(() => {
            const toAdd = [];
            const toUpdate = [];
            const toRemove = [];
            e.forEach(v => {
              if (this.gridOptions.api.getRowNode(v.postId) == null) {
                toAdd.push(v);
              } else {
                toUpdate.push(v);
              }
            });
            a.forEach(v => {
              if (v.stateAction === 'REMOVE') {
                toRemove.push(this.gridOptions.api.getRowNode(v.payload).data);
              }
            });
            this.gridOptions.api.updateRowData({ update: toUpdate, add: toAdd, remove: toRemove });
          });
        })
      );
    });

    this.subscriptions.push(
      this.wsConnectionService.state.subscribe(e => {
        this.websocketHandlerPromise.then((handler: WsHandler) => {
          handler.send(new WSMessage(CMD.POSTS_SUB.toString()));
        });
      })
    );
  }

  disconnect() {
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      handler.send(new WSMessage(CMD.POSTS_UNSUB.toString()));
    });
  }
}
