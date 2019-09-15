import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { PostsDataSource } from './post.datasource';
import { WsConnectionService } from '../ws-connection.service';
import { GridOptions, IFilterComp } from 'ag-grid-community';
import { PostProgressRendererComponent } from './post-progress.renderer.component';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-posts',
  templateUrl: './posts.component.html',
  styleUrls: ['./posts.component.scss']
})
export class PostsComponent implements OnInit, OnDestroy {
  constructor(private wsConnection: WsConnectionService, private zone: NgZone) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Posts',
          field: 'title',
          sortable: true,
          cellRenderer: 'progressCellRenderer',
          cellClass: 'no-padding',
          sort: 'asc'
        }
      ],
      rowHeight: 48,
      animateRows: true,
      rowData: [],
      frameworkComponents: {
        progressCellRenderer: PostProgressRendererComponent
      },
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['postId'],
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

  search(event) {
    this.gridOptions.api.setQuickFilter(event);
  }

  removeRows(postIds: string[]): void {
    if (postIds == null) {
      return;
    }

    const toRemove = [];

    postIds.forEach(p => {
      const nodeToDelete = this.gridOptions.api.getRowNode(p);
      if (nodeToDelete != null) {
        toRemove.push(nodeToDelete.data);
      }
    });
    this.gridOptions.api.updateRowData({ remove: toRemove });
  }

  ngOnInit() {}

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }
}
