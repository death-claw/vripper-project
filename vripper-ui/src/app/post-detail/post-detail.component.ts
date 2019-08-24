import { PostDetailsProgressRendererComponent } from './post-details-progress.component';
import { WsConnectionService } from './../ws-connection.service';
import { Component, OnInit, Inject, OnDestroy, NgZone } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { PostDetailsDataSource } from './post-details.datasource';
import { PostState } from '../posts/post-state.model';
import { GridOptions } from 'ag-grid-community';

@Component({
  selector: 'app-post-detail',
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.scss']
})
export class PostDetailComponent implements OnInit, OnDestroy {

  constructor(
    public dialogRef: MatDialogRef<PostDetailComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: PostState,
    private wsConnection: WsConnectionService,
    private zone: NgZone) {

    this.gridOptions = <GridOptions> {
      columnDefs: [
        {
          headerName: 'URL',
          field: 'url',
          sortable: true,
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
        this.dataSource = new PostDetailsDataSource(this.wsConnection, this.gridOptions, this.dialogData.postId, this.zone);
        this.dataSource.connect();
      },
      onGridSizeChanged: () => this.gridOptions.api.sizeColumnsToFit(),
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit()
    };

  }

  gridOptions: GridOptions;
  dataSource: PostDetailsDataSource;

  ngOnInit() {
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }

}
