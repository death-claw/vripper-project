import { CMD } from './../common/cmd.enum';
import { WSMessage } from './../common/ws-message.model';
import { Subject, Subscription } from 'rxjs';
import { WsConnectionService, WSState } from '../ws-connection.service';
import { GridOptions } from 'ag-grid-community';

export class PostsDataSource {

  constructor(private websocketConnection: WsConnectionService,
    private gridOptions: GridOptions) {
    this.websocket = websocketConnection.getConnection();
  }

  websocket: Subject<any>;

  subscriptions: Subscription[] = [];

  connect() {

    console.log('Connecting to posts datasource');
    this.subscriptions.push(this.websocketConnection.subscribeForPosts(e => {
      const toAdd = [];
      const toUpdate = [];
      e.forEach(v => {
        if (this.gridOptions.api.getRowNode(v.postId) == null) {
          toAdd.push(v);
        } else {
          toUpdate.push(v);
        }
      });
      this.gridOptions.api.updateRowData({ update: toUpdate, add: toAdd });
    }));

    this.subscriptions.push(this.websocketConnection.state.subscribe((e) => {
      if (e === WSState.OPEN) {
        this.websocket.next(new WSMessage(CMD.POSTS_SUB.toString()));
      }
    }));
  }

  disconnect() {
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
    this.websocket.next(new WSMessage(CMD.POSTS_UNSUB.toString()));
  }
}
