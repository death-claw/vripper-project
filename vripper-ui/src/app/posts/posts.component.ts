import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { PostsDataSource } from './post.datasource';
import { WsConnectionService } from '../ws-connection.service';
import { GridOptions } from 'ag-grid-community';
import { PostProgressRendererComponent } from './post-progress.renderer.component';
import { MenuRendererComponent } from './menu.renderer.component';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-posts',
  templateUrl: './posts.component.html',
  styleUrls: ['./posts.component.scss']
})
export class PostsComponent implements OnInit, OnDestroy {

  constructor(
    private wsConnection: WsConnectionService,
    private zone: NgZone
    ) {
    this.gridOptions = <GridOptions> {
      columnDefs: [
        {
          headerName: 'Title',
          field: 'title',
          sortable: true,
          tooltipField: 'title',
          cellRenderer: 'progressCellRenderer',
          cellClass: 'no-padding'
        },
        {
          headerName: 'Menu',
          cellRenderer: 'menuCellRenderer',
          sortable: true,
          width: 60,
          minWidth: 60,
          maxWidth: 60,
          suppressAutoSize: true
        }
      ],
      rowHeight: 48,
      rowData: [],
      frameworkComponents: {
        progressCellRenderer: PostProgressRendererComponent,
        menuCellRenderer: MenuRendererComponent
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: (data) => data['postId'],
      onGridReady: () => {
        this.gridOptions.api.sizeColumnsToFit();
        this.dataSource = new PostsDataSource(this.wsConnection, this.gridOptions, this.zone);
        this.dataSource.connect();
      },
      onGridSizeChanged: () => this.gridOptions.api.sizeColumnsToFit(),
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit()
    };
  }

  dialogOpen: Subject<boolean> = new Subject();
  gridOptions: GridOptions;
  dataSource: PostsDataSource;

  ngOnInit() {
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }
}
