import {Injectable} from '@angular/core';
import {GridApi} from 'ag-grid-community';
import {BehaviorSubject, Observable, Subject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PostsService {
  private api: GridApi;

  private _count$: Subject<number> = new BehaviorSubject(0);

  get count(): Observable<number> {
    return this._count$.asObservable();
  }

  setCount(count: number) {
    this._count$.next(count);
  }

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
    if (this.api) {
      this.api.setQuickFilter(event);
    }
  }
}
