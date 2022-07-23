import {WsConnectionService} from '../services/ws-connection.service';
import {ChangeDetectionStrategy, Component, Inject, NgZone, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PhotosDatasource} from './photos.datasource';
import {Post} from '../domain/post-state.model';
import {GridApi, GridOptions} from 'ag-grid-community';
import {ProgressRendererNative} from '../grid-custom-cells/progress-renderer.native';
import {StatusRendererNative} from '../grid-custom-cells/status-renderer.native';
import {UrlRendererNative} from '../grid-custom-cells/url-renderer.native';

@Component({
  selector: 'app-post-detail',
  templateUrl: './photos.component.html',
  styleUrls: ['./photos.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhotosComponent implements OnInit, OnDestroy {

  gridOptions: GridOptions;
  dataSource!: PhotosDatasource;
  private gridApi!: GridApi;

  constructor(
    public dialogRef: MatDialogRef<PhotosComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: Post,
    private wsConnection: WsConnectionService,
    private zone: NgZone
  ) {

    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: '#',
          field: 'index',
          sort: 'asc',
          width: 150,
          minWidth: 150
        }, {
          headerName: 'URL',
          field: 'url',
          cellRenderer: 'urlCellRenderer',
          flex: 1
        }, {
          headerName: 'Progress',
          field: 'progress',
          cellRenderer: 'nativeProgressCellRenderer',
          flex: 2
        }, {
          headerName: 'Status',
          field: 'status',
          cellRenderer: 'nativeStatusCellRenderer',
          width: 150,
          maxWidth: 150
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
        nativeProgressCellRenderer: ProgressRendererNative,
        nativeStatusCellRenderer: StatusRendererNative,
        urlCellRenderer: UrlRendererNative
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowId: (row) => row.data['url'],
      onGridReady: (params) => {
        this.gridApi = params.api;
        this.dataSource = new PhotosDatasource(this.wsConnection, this.gridApi, this.dialogData.postId, this.zone);
        this.dataSource.connect();
      },
    };
  }

  ngOnInit() {
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }
}
