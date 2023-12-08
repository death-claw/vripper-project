import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import { GridOptions, IRowNode, RowDataUpdatedEvent } from 'ag-grid-community';
import { Observable, Subscription } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { Log } from '../domain/log.model';

@Component({
  selector: 'app-log-table',
  standalone: true,
  imports: [CommonModule, AgGridModule],
  templateUrl: './log-table.component.html',
  styleUrls: ['./log-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogTableComponent implements OnInit, OnDestroy {
  @ViewChild('agGrid') agGrid!: AgGridAngular;

  @Output()
  rowCountChange = new EventEmitter<number>();

  @Input({ required: true })
  clear!: Observable<void>;

  gridOptions: GridOptions;
  subscriptions: Subscription[] = [];

  constructor(private applicationEndpoint: ApplicationEndpointService) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Time',
          field: 'time',
          tooltipField: 'time',
          sort: 'desc',
          flex: 1,
        },
        {
          headerName: 'Type',
          field: 'type',
          tooltipField: 'type',
          valueGetter: params => {
            switch (params.data.type) {
              case 'POST':
                return '🖼️ New gallery';
              case 'THREAD':
                return '🧵 New thread';
              case 'THANKS':
                return '👍 Sending a like ';
              case 'SCAN':
                return '🔍 Links scan';
              case 'METADATA':
              case 'METADATA_CACHE_MISS':
                return '🗄️ Loading post metadata';
              case 'QUEUED':
              case 'QUEUED_CACHE_MISS':
                return '📋 Loading multi-post link';
              case 'DOWNLOAD':
                return '📥 Download';
              default:
                return params.data.type;
            }
          },
          flex: 1,
        },
        {
          headerName: 'Status',
          field: 'status',
          tooltipField: 'status',
          flex: 1,
        },
        {
          headerName: 'Message',
          field: 'message',
          tooltipField: 'message',
          flex: 1,
        },
      ],
      defaultColDef: {
        sortable: true,
        resizable: true,
      },
      rowSelection: 'single',
      getRowId: row => row.data['id'].toString(),
      onGridReady: () => this.connect(),
      onRowDataUpdated: (event: RowDataUpdatedEvent) =>
        this.rowCountChange.emit(event.api.getDisplayedRowCount()),
    };
  }

  ngOnInit(): void {
    this.clear.subscribe(() => this.agGrid.api.setRowData([]));
  }

  private connect() {
    this.subscriptions.push(
      this.applicationEndpoint.newLogs$.subscribe((e: Log[]) => {
        this.agGrid.api.applyTransaction({ add: e });
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.updatedLogs$.subscribe((e: Log[]) => {
        const toUpdate: Log[] = [];
        e.forEach(v => {
          if (this.agGrid.api.getRowNode(v.id.toString()) != null) {
            toUpdate.push(v);
          }
        });
        this.agGrid.api.applyTransaction({ update: toUpdate });
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.logsRemove$.subscribe((e: number[]) => {
        const toRemove: any[] = [];
        e.forEach(v => {
          const rowNode: IRowNode | undefined = this.agGrid.api.getRowNode(
            v.toString()
          );
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
        });
        this.agGrid.api.applyTransaction({ remove: toRemove });
      })
    );
  }

  private disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
