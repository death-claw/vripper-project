import { element } from 'protractor';
import { GrabQueueState } from './grab-queue/grab-queue.model';
import { LoggedUser } from './common/logged-user.model';
import { Subject, Subscription } from 'rxjs';
import { WSMessage } from './common/ws-message.model';
import { PostState } from './posts/post-state.model';
import { map, filter } from 'rxjs/operators';
import { PostDetails } from './post-detail/post-details.model';
import { GlobalState } from './common/global-state.model';
import { DownloadSpeed } from './common/download-speed.model';

export class WsHandler {

}
