import {Injectable} from '@angular/core';
import {MultiPostItemsComponent} from '../multi-post-items/multi-post-items.component';
import {MultiPostModel} from '../domain/multi-post.model';
import {MatDialog} from '@angular/material/dialog';
import {HttpClient} from '@angular/common/http';
import {ServerService} from './server-service';
import {GridApi} from 'ag-grid-community';

interface ThreadId {
  threadId: string;
}

@Injectable({
  providedIn: 'root'
})
export class MultiPostService {

  constructor(private dialog: MatDialog,
              private httpClient: HttpClient,
              private serverService: ServerService) {
  }

  private static removeFromGrid(api: GridApi, data: ThreadId) {
    const removeTx = [];
    const nodeToDelete = api.getRowNode(data.threadId);
    if (nodeToDelete != null) {
      removeTx.push(nodeToDelete.data);
    }
    api.applyTransaction({remove: removeTx});
  }

  selectItems(multiPostModel: MultiPostModel) {
    this.dialog.open(MultiPostItemsComponent, {
      width: '90%',
      height: '90%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: multiPostModel
    });
  }

  remove(api: GridApi, multiPostModel: MultiPostModel) {
    this.httpClient.post<ThreadId>(this.serverService.baseUrl + '/grab/remove', {threadId: multiPostModel.threadId}).subscribe(
      data => {
        MultiPostService.removeFromGrid(api, data);
      }
    );
  }

}
