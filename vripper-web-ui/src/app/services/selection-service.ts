import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {IRowNode} from 'ag-grid-community';

@Injectable({
  providedIn: 'root'
})
export class SelectionService {

  private _selected$: Subject<IRowNode[]> = new Subject();

  get selected$(): Observable<IRowNode[]> {
    return this._selected$.asObservable();
  }

  public onSelectionChanged(selected: IRowNode[]) {
    this._selected$.next(selected);
  }
}
