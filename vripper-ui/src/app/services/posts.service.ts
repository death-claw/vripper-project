import {Injectable} from '@angular/core';
import {GridApi} from 'ag-grid-community';

@Injectable({
  providedIn: 'root'
})
export class PostsService {
  private api: GridApi;

  public setGridApi(api: GridApi) {
    this.api = api;
  }

  public remove(toRemove: { postId: string }[]) {
    const removeTx = [];
    toRemove.forEach(element => {
      const nodeToDelete = this.api.getRowNode(element.postId);
      if (nodeToDelete != null) {
        removeTx.push(nodeToDelete.data);
      }
    });
    this.api.applyTransaction({remove: removeTx});
  }

  search(event) {
    this.api.setQuickFilter(event);
  }
}
