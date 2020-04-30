import {PostDetailsProgressRendererComponent} from './renderer/post-details-progress.component';
import {WsConnectionService} from './../ws-connection.service';
import {ChangeDetectionStrategy, Component, Inject, NgZone, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PostDetailDatasource} from './post-detail.datasource';
import {PostState} from '../domain/post-state.model';
import {GridOptions} from 'ag-grid-community';
import {CtxtMenuService} from "./context-menu/ctxt-menu.service";

@Component({
  selector: 'app-post-detail',
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostDetailComponent implements OnInit, OnDestroy {

  constructor(
    public dialogRef: MatDialogRef<PostDetailComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: PostState,
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private contextMenuService: CtxtMenuService
  ) {

    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: '#',
          field: 'index',
          sortable: true,
          sort: 'asc',
          suppressMovable: true,
          width: 10,
          maxWidth: 50
        },
        {
          headerName: 'URL',
          field: 'url',
          sortable: true,
          suppressMovable: true,
          cellRenderer: 'progressCellRenderer',
          cellClass: 'no-padding'
        }
      ],
      rowHeight: 48,
      animateRows: true,
      rowData: [],
      frameworkComponents: {
        progressCellRenderer: PostDetailsProgressRendererComponent
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: (data) => data['url'],
      onGridReady: () => {
        this.gridOptions.api.sizeColumnsToFit();
        this.dataSource = new PostDetailDatasource(this.wsConnection, this.gridOptions, this.dialogData.postId, this.zone);
        this.dataSource.connect();
      },
      onGridSizeChanged: () => this.gridOptions.api.sizeColumnsToFit(),
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit(),
      onBodyScroll: () => this.contextMenuService.closePostDetailsCtxtMenu()
    };

  }

  gridOptions: GridOptions;
  dataSource: PostDetailDatasource;

  ngOnInit() {
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }

}
