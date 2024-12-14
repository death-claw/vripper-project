import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, Observable, Subject, Subscription } from 'rxjs';
import { Log } from '../domain/log.model';
import { Settings } from '../domain/settings.model';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { isDisplayed } from '../utils/utils';

@Component({
  selector: 'app-log-table',
  imports: [CommonModule, MatCheckboxModule, MatTableModule],
  templateUrl: './log-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class LogTableComponent implements OnInit {
  dataSource = new LogDataSource(this.applicationEndpoint);
  displayedColumns: string[] = [
    'timestamp',
    'threadName',
    'loggerName',
    'levelString',
    'formattedMessage',
  ];
  selection = new SelectionModel<Log>(
    false,
    [],
    true,
    (a, b) => a.sequence === b.sequence
  );
  isDisplayed = isDisplayed;
  columnsToDisplay = signal([...this.displayedColumns]);

  rowCountChange = output<number>();

  @Input({ required: true })
  clear!: Subject<void>;

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private breakpointObserver: BreakpointObserver
  ) {
    this.dataSource._dataStream.subscribe((v) =>
      this.rowCountChange.emit(v.length)
    );
    this.breakpointObserver
      .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium])
      .subscribe((result) => {
        if (result.matches) {
          this.columnsToDisplay.set([
            'timestamp',
            'levelString',
            'formattedMessage',
          ]);
        } else {
          this.columnsToDisplay.set([
            'timestamp',
            'threadName',
            'loggerName',
            'levelString',
            'formattedMessage',
          ]);
        }
      });
  }

  ngOnInit(): void {
    this.clear.subscribe((e) => this.dataSource._dataStream.next([]));
  }

  onClick(row: Log) {
    this.selection.clear();
    this.selection.select(row);
  }
}

class LogDataSource extends DataSource<Log> {
  subscriptions: Subscription[] = [];
  _dataStream = new BehaviorSubject<Log[]>([]);
  maxLog: number = 0;

  constructor(private applicationEndpoint: ApplicationEndpointService) {
    super();
  }

  connect(): Observable<Log[]> {
    this.subscriptions.push(
      this.applicationEndpoint.newLogs$.subscribe((newLog: Log) => {
        if (this._dataStream.value.length >= this.maxLog) {
          // remove first element
          const newval = [...this._dataStream.value].sort(
            (a, b) => a.sequence - b.sequence
          );
          newval.splice(0, newval.length - this.maxLog + 1);

          this._dataStream.next([...newval, newLog]);
        } else {
          this._dataStream.next([...this._dataStream.value, newLog]);
        }
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.settingsUpdate$.subscribe(
        (settings: Settings) => {
          this.maxLog = settings.systemSettings.maxEventLog;
        }
      )
    );

    this.subscriptions.push(
      this.applicationEndpoint
        .settings()
        .subscribe((v) => (this.maxLog = v.systemSettings.maxEventLog))
    );

    return this._dataStream.asObservable();
  }

  disconnect(): void {
    this.subscriptions.forEach((e) => e.unsubscribe());
  }
}
