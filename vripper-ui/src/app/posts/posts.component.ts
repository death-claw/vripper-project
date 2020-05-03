import {PostsDataService} from './posts-data.service';
import {SelectionService} from '../selection-service';
import {AfterViewInit, ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {PostsDataSource} from './post.datasource';
import {WsConnectionService} from '../ws-connection.service';
import {GridOptions} from 'ag-grid-community';
import {PostProgressRendererComponent} from './renderer/post-progress.renderer.component';
import {Subject} from 'rxjs';
import {CtxtMenuService} from "./context-menu/ctxt-menu.service";

@Component({
  selector: 'app-posts',
  templateUrl: './posts.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostsComponent implements OnInit, OnDestroy, AfterViewInit {
  constructor(
    private wsConnection: WsConnectionService,
    private zone: NgZone,
    private selectionService: SelectionService,
    private postsDataService: PostsDataService,
    private contextMenuService: CtxtMenuService
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Galleries',
          field: 'title',
          sortable: true,
          cellRenderer: 'progressCellRenderer',
          cellClass: 'no-padding',
          sort: 'asc',
          suppressMovable: true,
          headerCheckboxSelection: true,
          headerCheckboxSelectionFilteredOnly: true
        }
      ],
      rowHeight: 60,
      animateRows: true,
      rowSelection: 'multiple',
      rowDeselection: true,
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
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit(),
      onSelectionChanged: () => this.selectionService.onSelectionChanged(this.gridOptions.api.getSelectedNodes()),
      onBodyScroll: () => this.contextMenuService.closePostCtxtMenu()
    };
  }

  dialogOpen: Subject<boolean> = new Subject();
  gridOptions: GridOptions;
  dataSource: PostsDataSource;

  ngOnInit() {}

  ngAfterViewInit(): void {
    this.postsDataService.setGridApi(this.gridOptions.api);
  }

  ngOnDestroy(): void {
    this.dataSource.disconnect();
  }
}
