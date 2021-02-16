import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {GridApi} from 'ag-grid-community';

@Injectable({
  providedIn: 'root'
})
export class LinkCollectorService {
  private api: GridApi;

  private _count$: Subject<number> = new BehaviorSubject(0);

  get count(): Observable<number> {
    return this._count$.asObservable();
  }

  public setGridApi(api: GridApi) {
    this.api = api;
  }

  search(event) {
    if (this.api) {
      this.api.setQuickFilter(event);
    }
  }

  setCount(count: number) {
    this._count$.next(count);
  }

  clear() {
    if (this.api) {
      this.api.setRowData([]);
    }
  }
}
