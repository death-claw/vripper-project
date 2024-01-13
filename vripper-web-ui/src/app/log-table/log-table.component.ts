import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { Log } from '../domain/log.model';
import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { formatType, LogRow } from '../domain/log-row.model';
import { isDisplayed } from '../utils/utils';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';

@Component({
  selector: 'app-log-table',
  standalone: true,
  imports: [CommonModule, MatCheckboxModule, MatTableModule],
  templateUrl: './log-table.component.html',
  styleUrls: ['./log-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogTableComponent implements OnInit {
  dataSource = new LogDataSource(this.applicationEndpoint);
  displayedColumns: string[] = ['time', 'type', 'status', 'message'];
  selection = new SelectionModel<LogRow>(
    false,
    [],
    true,
    (a, b) => a.id === b.id
  );
  isDisplayed = isDisplayed;
  columnsToDisplay = signal([...this.displayedColumns]);
  @Output()
  rowCountChange = new EventEmitter<number>();
  @Input({ required: true })
  clear!: Observable<void>;

  constructor(private applicationEndpoint: ApplicationEndpointService) {
    this.dataSource._dataStream.subscribe(v =>
      this.rowCountChange.emit(v.length)
    );
  }

  ngOnInit(): void {
    this.clear.subscribe(() => {
      this.dataSource._dataStream.next([]);
    });
  }

  onClick(row: LogRow) {
    this.selection.clear();
    this.selection.select(row);
  }
}

class LogDataSource extends DataSource<LogRow> {
  subscriptions: Subscription[] = [];
  _dataStream = new BehaviorSubject<LogRow[]>([]);

  constructor(private applicationEndpoint: ApplicationEndpointService) {
    super();
  }

  connect(): Observable<LogRow[]> {
    this.subscriptions.push(
      this.applicationEndpoint.newLogs$.subscribe((newLogs: Log[]) => {
        this._dataStream.next([
          ...this._dataStream.value,
          ...newLogs.map(
            e => new LogRow(e.id, e.type, e.status, e.time, e.message)
          ),
        ]);
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.logsRemove$.subscribe((e: number[]) => {
        this._dataStream.next([
          ...this._dataStream.value.filter(
            v => e.find(d => d === v.id) == null
          ),
        ]);
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.updatedLogs$.subscribe((e: Log[]) => {
        e.forEach(v => {
          const rowNode = this._dataStream.value.find(d => d.id === v.id);
          if (rowNode != null) {
            Object.assign(rowNode, v);
            rowNode.type = formatType(v.type);
            rowNode.statusSignal.set(v.status);
            rowNode.messageSignal.set(v.message);
          }
        });
      })
    );
    return this._dataStream.asObservable();
  }

  disconnect(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
