import { GridOptions } from 'ag-grid-community';
import { Subject, Subscription } from 'rxjs';
import { WsConnectionService, WSState } from '../ws-connection.service';
import { WSMessage } from '../common/ws-message.model';
import { CMD } from '../common/cmd.enum';
declare var SockJS;

export class PostDetailsDataSource {

    constructor(private websocketConnection: WsConnectionService, private gridOptions: GridOptions, private postId: string) {
        this.websocket = websocketConnection.getConnection();
    }

    websocket: Subject<any>;

    subscriptions: Subscription[] = [];

    connect() {

        console.log('Connecting to post details datasource');
        this.subscriptions.push(this.websocketConnection.subscribeForPostDetails(e => {
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
        }));

        this.subscriptions.push(this.websocketConnection.state.subscribe((e) => {
            if (e === WSState.OPEN) {
                this.websocket.next(new WSMessage(CMD.POST_DETAILS_SUB.toString(), this.postId));
            }
        }));
    }

    disconnect() {
        console.log('Disconnecting from post details datasource');
        this.subscriptions.forEach(e => e.unsubscribe());
        this.websocket.next(new WSMessage(CMD.POST_DETAILS_UNSUB.toString()));
    }
}
