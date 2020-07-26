import {finalize} from 'rxjs/operators';
import {VRPostParse} from '../domain/vr-post-parse.model';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Inject,
  NgZone,
  OnDestroy,
  OnInit
} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {GridOptions, RowNode} from 'ag-grid-community';
import {UrlRendererComponent} from './renderer/url-renderer.component';
import {BehaviorSubject, Subject, Subscription} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {ServerService} from '../server-service';
import {GrabQueueState} from '../domain/grab-queue.model';

@Component({
  selector: 'app-multi-post',
  templateUrl: './multi-post.component.html',
  styleUrls: ['./multi-post.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MultiPostComponent implements OnInit, OnDestroy, AfterViewInit {
  gridOptions: GridOptions;
  subscription: Subscription;
  loading: EventEmitter<boolean> = new EventEmitter();

  constructor(
    private ngZone: NgZone,
    private _snackBar: MatSnackBar,
    private httpClient: HttpClient,
    private serverService: ServerService,
    public dialogRef: MatDialogRef<MultiPostComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: GrabQueueState
  ) {}

  selectedRowsCount: Subject<number> = new BehaviorSubject(0);

  ngAfterViewInit(): void {
    this.loading.emit(true);
  }

  ngOnInit(): void {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Number',
          field: 'number',
          maxWidth: 100,
          suppressMovable: true,
          checkboxSelection: true,
          headerCheckboxSelection: true,
          headerCheckboxSelectionFilteredOnly: false,
          sort: 'asc'
        },
        {
          headerName: 'Title',
          field: 'title',
          suppressMovable: true,
          cellClass: 'no-padding'
        },
        {
          headerName: 'URL',
          field: 'url',
          cellClass: 'no-padding',
          suppressMovable: true,
          cellRenderer: 'urlCellRenderer'
        },
        {
          headerName: 'Hosts',
          field: 'hosts',
          suppressMovable: true,
          cellClass: 'no-padding'
        }
      ],
      defaultColDef: {
        sortable: true
      },
      frameworkComponents: {
        urlCellRenderer: UrlRendererComponent
      },
      rowSelection: 'multiple',
      rowMultiSelectWithClick: true,
      suppressRowClickSelection: true,
      rowHeight: 48,
      animateRows: true,
      rowData: [],
      overlayLoadingTemplate: '<span></span>',
      overlayNoRowsTemplate: '<span></span>',
      getRowNodeId: data => data['url'],
      onGridReady: () => {
        this.gridOptions.api.sizeColumnsToFit();
        this.grab();
      },
      onGridSizeChanged: () => this.gridOptions.api.sizeColumnsToFit(),
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit(),
      onSelectionChanged: () => this.onSelectionChange(this.gridOptions.api.getSelectedNodes())
    };
  }

  onSelectionChange(data: RowNode[]) {
    this.selectedRowsCount.next(data.length);
  }

  ngOnDestroy() {}

  search(event) {
    this.gridOptions.api.setQuickFilter(event);
  }

  submit() {
    this.loading.emit(true);
    const data = (<VRPostParse[]>this.gridOptions.api.getSelectedRows()).map(e => ({
      postId: e.postId,
      threadId: this.dialogData.threadId
    }));

    this.httpClient
      .post(this.serverService.baseUrl + '/post/add', data)
      .pipe(
        finalize(() => {
          this.dialogRef.close();
          this.loading.emit(false);
        })
      )
      .subscribe(
        () => {},
        error => {
          this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
  }

  grab() {
    this.httpClient
      .get<VRPostParse[]>(this.serverService.baseUrl + '/grab/' + this.dialogData.threadId)
      .pipe(finalize(() => this.loading.emit(false)))
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
    this.dialogRef.close();
  }
}
