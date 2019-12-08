import { finalize } from 'rxjs/operators';
import { VRPostParse } from './../common/vr-post-parse.model';
import {
  Component,
  OnInit,
  NgZone,
  OnDestroy,
  EventEmitter,
  ChangeDetectionStrategy,
  AfterViewInit,
  Inject
} from '@angular/core';
import { MatSnackBar, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { GridOptions } from 'ag-grid-community';
import { UrlRendererComponent } from './url-renderer.component';
import { WsHandler } from '../ws-handler';
import { Subscription, concat, merge } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ServerService } from '../server-service';
import { GrabQueueState } from '../grab-queue/grab-queue.model';

@Component({
  selector: 'app-multi-post',
  templateUrl: './multi-post.component.html',
  styleUrls: ['./multi-post.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MultiPostComponent implements OnInit, OnDestroy, AfterViewInit {
  gridOptions: GridOptions;
  websocketHandlerPromise: Promise<WsHandler>;
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

  ngAfterViewInit(): void {
    this.loading.emit(true);
  }

  ngOnInit(): void {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Number',
          field: 'number',
          cellClass: 'no-padding',
          maxWidth: 100,
          checkboxSelection: true,
          headerCheckboxSelection: true,
          headerCheckboxSelectionFilteredOnly: false,
          sort: 'asc'
        },
        {
          headerName: 'Title',
          field: 'title',
          cellClass: 'no-padding'
        },
        {
          headerName: 'URL',
          field: 'url',
          cellClass: 'no-padding',
          cellRenderer: 'urlCellRenderer'
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
      onRowDataUpdated: () => this.gridOptions.api.sizeColumnsToFit()
    };
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

    merge(
      this.httpClient.post(this.serverService.baseUrl + '/post/add', data),
      this.httpClient.post(this.serverService.baseUrl + '/grab/remove', { url: this.dialogData.link })
    )
      .pipe(finalize(() => {
        this.dialogRef.close();
        this.loading.emit(false);
      }))
      .subscribe(
        () => {},
        error => {
          this._snackBar.open(error.error, null, {
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
          this.gridOptions.api.updateRowData({ add: data });
        },
        error => {
          this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        }
      );
  }

  // removeFromQueue() {
  //   this.httpClient.post(this.serverService.baseUrl + '/grab/remove', { url: this.dialogData.link }).subscribe(
  //     () => {},
  //     error => {
  //       this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
  //         duration: 5000
  //       });
  //     }
  //   );
  // }

  // addPosts(data: { postId: string; threadId: string }[]) {
  //   this.httpClient.post(this.serverService.baseUrl + '/post/add', data).subscribe(() => {
  //     this._snackBar.open('Adding to download queue', null, {
  //       duration: 5000
  //     });
  //     this.dialogRef.close();
  //   });
  // }

  onNoClick() {
    this.dialogRef.close();
  }
}
