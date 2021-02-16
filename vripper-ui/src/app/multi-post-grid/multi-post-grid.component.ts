import {LinkCollectorService} from '../services/link-collector.service';
import {MultiPostGridDataSource} from './multi-post-grid-data.source';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy} from '@angular/core';
import {GridOptions} from 'ag-grid-community';
import {WsConnectionService} from '../services/ws-connection.service';
import {CollectorActionsRendererNative} from '../grid-custom-cells/collector-actions-renderer.native';
import {MultiPostService} from '../services/multi-post.service';
import {UrlRendererNative} from '../grid-custom-cells/url-renderer.native';
import {ElectronService} from 'ngx-electron';

@Component({
  selector: 'app-grab-queue',
  templateUrl: './multi-post-grid.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MultiPostGridComponent implements OnDestroy {

  gridOptions: GridOptions;
  dataSource: MultiPostGridDataSource;

  constructor(
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private linkCollectorService: LinkCollectorService,
    private grabQueueService: MultiPostService,
    private electronService: ElectronService) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Url',
          field: 'link',
          sort: 'asc',
          cellRenderer: 'urlCellRenderer',
          cellRendererParams: {
            electronService: this.electronService
          },
          flex: 1
        }, {
          headerName: 'Count',
          field: 'total',
          minWidth: 150,
          width: 150
        }, {
          headerName: 'Action',
          cellRenderer: 'actionsCellRenderer',
          cellRendererParams: {
            grabQueueService: this.grabQueueService
          },
          maxWidth: 200,
          width: 200
        }
      ],
      defaultColDef: {
        sortable: true,
        resizable: true
      },
      rowSelection: 'single',
      rowHeight: 26,
      headerHeight: 35,
      animateRows: true,
      rowData: [],
      components: {
        actionsCellRenderer: CollectorActionsRendererNative,
        urlCellRenderer: UrlRendererNative
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['threadId'],
      onGridReady: () => {
        this.linkCollectorService.setGridApi(this.gridOptions.api);
        this.dataSource = new MultiPostGridDataSource(this.wsConnection, this.gridOptions, this.zone);
        this.dataSource.connect();
      },
      onRowDataUpdated: () => this.linkCollectorService.setCount(this.gridOptions.api.getDisplayedRowCount()),
      onRowDataChanged: () => this.linkCollectorService.setCount(this.gridOptions.api.getDisplayedRowCount()),
    };
  }

  ngOnDestroy(): void {
    if (this.dataSource) {
      this.dataSource.disconnect();
    }
  }
}
