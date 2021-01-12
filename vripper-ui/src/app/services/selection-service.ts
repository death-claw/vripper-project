import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {RowNode} from 'ag-grid-community';

@Injectable({
  providedIn: 'root'
})
export class SelectionService {

  private _selected$: Subject<RowNode[]> = new Subject();

  get selected$(): Observable<RowNode[]> {
    return this._selected$.asObservable();
  }

  public onSelectionChanged(selected: RowNode[]) {
    this._selected$.next(selected);
  }
}
