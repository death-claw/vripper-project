import {finalize} from 'rxjs/operators';
import {MultiPostItem} from '../domain/vr-post-parse.model';
import {ChangeDetectionStrategy, Component, Inject, NgZone} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {GridOptions, RowNode} from 'ag-grid-community';
import {BehaviorSubject, Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {ServerService} from '../services/server-service';
import {MultiPostModel} from '../domain/multi-post.model';
import {TitleRendererNative} from '../grid-custom-cells/title-renderer.native';
import {PostContextMenuService} from '../services/post-context-menu.service';
import {Overlay, OverlayPositionBuilder} from '@angular/cdk/overlay';
import {UrlRendererNative} from '../grid-custom-cells/url-renderer.native';
import {ValueFormatterParams} from 'ag-grid-community/dist/lib/entities/colDef';
import {ElectronService} from 'ngx-electron';

@Component({
  selector: 'app-multi-post-items',
  templateUrl: './multi-post-items.component.html',
  styleUrls: ['./multi-post-items.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MultiPostItemsComponent {
  gridOptions: GridOptions;
  loading: Subject<boolean> = new BehaviorSubject(true);
  selectedRowsCount: Subject<number> = new BehaviorSubject(0);

  constructor(
    private ngZone: NgZone,
    private _snackBar: MatSnackBar,
    private httpClient: HttpClient,
    private serverService: ServerService,
    public dialogRef: MatDialogRef<MultiPostItemsComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: MultiPostModel,
    private contextMenuService: PostContextMenuService,
    private overlayPositionBuilder: OverlayPositionBuilder,
    private overlay: Overlay,
    private zone: NgZone,
    private electronService: ElectronService
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Number',
          field: 'number',
          checkboxSelection: true,
          headerCheckboxSelection: true,
          headerCheckboxSelectionFilteredOnly: false,
          sort: 'asc',
          minWidth: 150,
          width: 150
        },
        {
          headerName: 'Title',
          field: 'title',
          cellRenderer: 'titleCellRenderer',
          cellRendererParams: {
            contextMenuService: this.contextMenuService,
            overlayPositionBuilder: this.overlayPositionBuilder,
            overlay: this.overlay,
            zone: this.zone
          },
          flex: 1
        },
        {
          headerName: 'URL',
          field: 'url',
          cellRenderer: 'urlCellRenderer',
          cellRendererParams: {
            electronService: this.electronService
          },
          flex: 1
        },
        {
          headerName: 'Hosts',
          field: 'hosts',
          valueFormatter: (params: ValueFormatterParams) => params.value ? params.value : 'Unsupported',
          minWidth: 150,
          width: 150
        }
      ],
      defaultColDef: {
        sortable: true,
        resizable: true
      },
      components: {
        titleCellRenderer: TitleRendererNative,
        urlCellRenderer: UrlRendererNative
      },
      rowHeight: 26,
      headerHeight: 35,
      rowSelection: 'multiple',
      animateRows: true,
      rowData: [],
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['url'],
      onGridReady: () => this.fetchMultiPostItems(),
      onSelectionChanged: () => this.onSelectionChange(this.gridOptions.api.getSelectedNodes())
    };
  }

  onSelectionChange(data: RowNode[]) {
    this.selectedRowsCount.next(data.length);
  }

  search(event) {
    this.gridOptions.api.setQuickFilter(event);
  }

  submit() {
    const data = (<MultiPostItem[]>this.gridOptions.api.getSelectedRows()).map(e => ({
      postId: e.postId,
      threadId: this.dialogData.threadId
    }));

    this.httpClient
      .post(this.serverService.baseUrl + '/post/add', data)
      .pipe(
        finalize(() => {
          this.loading.next(false);
          this.dialogRef.close();
        })
      )
      .subscribe(
        () => {
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
  }

  fetchMultiPostItems() {
    this.httpClient
      .get<MultiPostItem[]>(this.serverService.baseUrl + '/grab/' + this.dialogData.threadId)
      .pipe(finalize(() => this.loading.next(false)))
      .subscribe(
        data => {
          this.gridOptions.api.applyTransaction({add: data});
        },
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
  }

  onNoClick() {
    this.ngZone.run(() => this.dialogRef.close());
  }
}
