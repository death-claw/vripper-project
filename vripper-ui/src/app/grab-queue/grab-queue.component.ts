import {LinkCollectorService} from './../link-collector.service';
import {UrlGrabRendererComponent} from './renderer/url-renderer.component';
import {GrabQueueDataSource} from './grab-queue.datasource';
import {ChangeDetectionStrategy, Component, NgZone, OnInit} from '@angular/core';
import {GridOptions} from 'ag-grid-community';
import {WsConnectionService} from '../ws-connection.service';
import {NotificationService} from '../notification.service';

@Component({
  selector: 'app-grab-queue',
  templateUrl: './grab-queue.component.html',
  styleUrls: ['./grab-queue.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrabQueueComponent implements OnInit {
  constructor(
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private linkCollectorService: LinkCollectorService,
    private notificationService: NotificationService) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Url',
          field: 'link',
          sortable: true,
          sort: 'asc',
          suppressMovable: true,
          cellRenderer: 'urlCellRenderer'
        }
      ],
      rowHeight: 50,
      animateRows: true,
      rowData: [],
      frameworkComponents: {
        urlCellRenderer: UrlGrabRendererComponent
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['threadId'],
      onGridReady: () => {
        this.gridOptions.api.sizeColumnsToFit();
        this.dataSource = new GrabQueueDataSource(this.wsConnection, this.gridOptions, this.zone, this.notificationService);
        this.dataSource.connect();
      },
      onGridSizeChanged: () => this.gridOptions.api.sizeColumnsToFit(),
      onRowDataUpdated: () => {
        this.linkCollectorService.setCount(this.gridOptions.api.getDisplayedRowCount());
        this.gridOptions.api.sizeColumnsToFit();
      }
    };
  }

  gridOptions: GridOptions;
  dataSource: GrabQueueDataSource;

  ngOnInit() {}
}
