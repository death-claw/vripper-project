import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {GridApi} from 'ag-grid-community';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {EventLogMessageDialogComponent} from '../event-log/message-dialog/event-log-message-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class EventLogService {
  private api: GridApi;

  private _count$: Subject<number> = new BehaviorSubject(0);

  constructor(public dialog: MatDialog) {
  }

  get count(): Observable<number> {
    return this._count$.asObservable();
  }

  setCount(count: number) {
    this._count$.next(count);
  }

  public setGridApi(api: GridApi) {
    this.api = api;
  }

  search(event) {
    if (this.api) {
      this.api.setQuickFilter(event);
    }
  }

  clear() {
    if (this.api) {
      this.api.setRowData([]);
    }
  }

  openDialog(message: string) {
    console.log(message);
    const config: MatDialogConfig<string> = {
      width: '70%',
      height: '70%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: message
    };
    this.dialog.open(EventLogMessageDialogComponent, config);
  }
}
