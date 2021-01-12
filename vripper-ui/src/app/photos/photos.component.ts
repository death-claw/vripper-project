import {WsConnectionService} from '../services/ws-connection.service';
import {ChangeDetectionStrategy, Component, Inject, NgZone, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PhotosDatasource} from './photos.datasource';
import {Post} from '../domain/post-state.model';
import {GridOptions} from 'ag-grid-community';
import {ProgressRendererNative} from '../grid-custom-cells/progress-renderer.native';
import {StatusRendererNative} from '../grid-custom-cells/status-renderer.native';
import {UrlRendererNative} from '../grid-custom-cells/url-renderer.native';
import {ElectronService} from 'ngx-electron';

@Component({
  selector: 'app-post-detail',
  templateUrl: './photos.component.html',
  styleUrls: ['./photos.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhotosComponent implements OnInit, OnDestroy {

  gridOptions: GridOptions;
  dataSource: PhotosDatasource;

  constructor(
    public dialogRef: MatDialogRef<PhotosComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: Post,
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private electronService: ElectronService
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
          cellRendererParams: {
            electronService: this.electronService
          },
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
      getRowNodeId: (data) => data['url'],
      onGridReady: () => {
        this.dataSource = new PhotosDatasource(this.wsConnection, this.gridOptions, this.dialogData.postId, this.zone);
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
