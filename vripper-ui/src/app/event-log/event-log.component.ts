import {ChangeDetectionStrategy, Component, NgZone, OnDestroy} from '@angular/core';
import {GridOptions} from 'ag-grid-community';
import {WsConnectionService} from '../services/ws-connection.service';
import {EventLogDatasource} from './event-log.datasource';
import {StatusRendererNative} from '../grid-custom-cells/status-renderer.native';
import {EventLogService} from '../services/event-log.service';
import {EventMessageRendererNative} from '../grid-custom-cells/event-message-renderer.native';

@Component({
  selector: 'app-event-log',
  templateUrl: './event-log.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EventLogComponent implements OnDestroy {
  gridOptions: GridOptions;
  dataSource: EventLogDatasource;

  constructor(
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private eventLogService: EventLogService
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Time',
          field: 'time',
          sort: 'desc',
          width: 250,
          maxWidth: 250
        }, {
          headerName: 'Type',
          field: 'type',
          valueGetter: (params => {
            switch (params.data.type) {
              case 'POST':
                return 'New gallery';
              case 'QUEUED':
                return 'New multi-post link';
              case 'THANKS':
                return 'Leave thanks';
              case 'SCAN':
                return 'Links scan';
              case 'METADATA':
                return 'Fetching metadata';
              case 'METADATA_CACHE_MISS':
                return 'Loading post metadata';
              case 'QUEUED_CACHE_MISS':
                return 'Loading multi-post links';
              case 'DOWNLOAD':
                return 'Download';
              default:
                return params.data.type;
            }
          }),
          width: 250,
          maxWidth: 250
        }, {
          headerName: 'Status',
          field: 'status',
          cellRenderer: 'nativeStatusCellRenderer',
          width: 150,
          maxWidth: 150
        }, {
          headerName: 'Message',
          field: 'message',
          flex: 1,
          cellRenderer: 'messageCellRenderer',
          cellRendererParams: {
            eventLogService: this.eventLogService
          },
        }
      ],
      defaultColDef: {
        sortable: true,
        resizable: true
      },
      rowHeight: 26,
      headerHeight: 35,
      animateRows: true,
      rowSelection: 'single',
      rowDeselection: true,
      rowData: [],
      components: {
        nativeStatusCellRenderer: StatusRendererNative,
        messageCellRenderer: EventMessageRendererNative,
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['id'],
      onGridReady: () => {
        this.eventLogService.setGridApi(this.gridOptions.api);
        this.dataSource = new EventLogDatasource(this.wsConnection, this.gridOptions, this.zone);
        this.dataSource.connect();
      },
      onRowDataUpdated: () => this.eventLogService.setCount(this.gridOptions.api.getDisplayedRowCount()),
      onRowDataChanged: () => this.eventLogService.setCount(this.gridOptions.api.getDisplayedRowCount()),
    };
  }

  ngOnDestroy(): void {
    if (this.dataSource) {
      this.dataSource.disconnect();
    }
  }
}
