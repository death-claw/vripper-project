import {
  Component,
  ComponentRef,
  EventEmitter,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import {
  CellContextMenuEvent,
  GridOptions,
  IRowNode,
  RowDataUpdatedEvent,
  RowDoubleClickedEvent,
  SelectionChangedEvent,
  ValueFormatterParams,
} from 'ag-grid-community';
import { Post } from '../domain/post.model';
import { fromEvent, merge, Subscription, take } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import {
  Overlay,
  OverlayModule,
  OverlayPositionBuilder,
} from '@angular/cdk/overlay';
import { ComponentPortal, PortalModule } from '@angular/cdk/portal';
import { PostContextmenuComponent } from '../post-contextmenu/post-contextmenu.component';
import { ValueGetterParams } from 'ag-grid-community/dist/lib/entities/colDef';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {
  ConfirmComponent,
  ConfirmDialogData,
} from '../confirm/confirm.component';
import { ProgressCellComponent } from '../progress-cell/progress-cell.component';
import { Image } from '../domain/image.model';
import { ImageDialogData, ImagesComponent } from '../images/images.component';
import { formatBytes } from '../utils/utils';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({
  selector: 'app-download-table',
  standalone: true,
  imports: [
    CommonModule,
    AgGridModule,
    OverlayModule,
    PortalModule,
    MatDialogModule,
    ProgressCellComponent,
    MatListModule,
  ],
  templateUrl: './download-table.component.html',
  styleUrls: ['./download-table.component.scss'],
})
export class DownloadTableComponent implements OnDestroy {
  @ViewChild('agGrid') agGrid!: AgGridAngular;
  gridOptions: GridOptions;

  @Output()
  rowCountChange = new EventEmitter<number>();

  @Output()
  selectedChange = new EventEmitter<Post[]>();

  subscriptions: Subscription[] = [];

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private overlayPositionBuilder: OverlayPositionBuilder,
    private overlay: Overlay,
    private dialog: MatDialog,
    breakpointObserver: BreakpointObserver
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          colId: 'title',
          headerName: 'Title',
          field: 'postTitle',
          tooltipField: 'postTitle',
          headerCheckboxSelection: true,
          headerCheckboxSelectionFilteredOnly: true,
          flex: 2,
          minWidth: 200,
        },
        {
          colId: 'progress',
          headerName: 'Progress',
          tooltipValueGetter: params => {
            if (!params.data) {
              return 0;
            }
            return params.data.done === 0 || params.data.total === 0
              ? 0
              : (params.data.done / params.data.total) * 100;
          },
          valueGetter: (params: ValueGetterParams<Post>) => {
            if (!params.data) {
              return 0;
            }
            return params.data.done === 0 || params.data.total === 0
              ? 0
              : (params.data.done / params.data.total) * 100;
          },
          cellRenderer: ProgressCellComponent,
          flex: 1,
          minWidth: 100,
        },
        {
          colId: 'status',
          headerName: 'Status',
          field: 'status',
          tooltipValueGetter: params => {
            const value = params.value as string;
            return (
              value.at(0)?.toUpperCase() +
              value.substring(1, value.length).toLowerCase()
            );
          },
          valueFormatter: (params: ValueFormatterParams<Image>) => {
            const value = params.value as string;
            return (
              value.at(0)?.toUpperCase() +
              value.substring(1, value.length).toLowerCase()
            );
          },
          flex: 1,
        },
        {
          colId: 'path',
          headerName: 'Path',
          field: 'downloadDirectory',
          tooltipField: 'downloadDirectory',
          flex: 1,
        },
        {
          colId: 'total',
          headerName: 'Total',
          tooltipValueGetter: params =>
            `${params.data?.done}/${params.data?.total} (${formatBytes(
              params.data?.downloaded
            )})`,
          valueGetter: params =>
            `${params.data?.done}/${params.data?.total} (${formatBytes(
              params.data?.downloaded
            )})`,
          flex: 1,
        },
        {
          colId: 'hosts',
          headerName: 'Hosts',
          field: 'hosts',
          tooltipField: 'hosts',
          valueGetter: (params: ValueGetterParams<Post>) => {
            return params.data?.hosts.join(', ');
          },
          flex: 1,
        },
        {
          colId: 'addedOn',
          headerName: 'Added On',
          field: 'addedOn',
          tooltipField: 'addedOn',
          flex: 1,
        },
        {
          colId: 'order',
          headerName: 'Order',
          field: 'rank',
          tooltipField: 'rank',
          sort: 'asc',
          flex: 0.5,
        },
      ],
      defaultColDef: {
        sortable: true,
        resizable: true,
      },
      rowSelection: 'multiple',
      getRowId: row => row.data['postId'],
      onGridReady: () => {
        this.connect();
        breakpointObserver
          .observe(Breakpoints.HandsetPortrait)
          .subscribe(result => {
            this.agGrid.columnApi.setColumnsVisible(
              ['status', 'total', 'hosts', 'path', 'addedOn', 'order'],
              !result.matches
            );
          });
      },
      onRowDataUpdated: (event: RowDataUpdatedEvent<Post>) =>
        this.rowCountChange.emit(event.api.getDisplayedRowCount()),
      onCellContextMenu: (event: CellContextMenuEvent<Post>) => {
        if (event.api.getSelectedRows().length > 1) {
          event.node.setSelected(true);
        } else {
          event.node.setSelected(true, true);
        }
        const mouseEvent = event.event as MouseEvent;
        const positionStrategy = this.overlayPositionBuilder
          .flexibleConnectedTo({ x: mouseEvent.x, y: mouseEvent.y })
          .withPush(true)
          .withGrowAfterOpen(true)
          .withPositions([
            {
              originX: 'start',
              originY: 'bottom',
              overlayX: 'start',
              overlayY: 'top',
            },
            {
              originX: 'start',
              originY: 'top',
              overlayX: 'start',
              overlayY: 'bottom',
            },
          ]);
        const postContextMenuOverlayRef = this.overlay.create({
          positionStrategy,
        });
        const postContextMenuPortal = new ComponentPortal(
          PostContextmenuComponent
        );
        const ref: ComponentRef<PostContextmenuComponent> =
          postContextMenuOverlayRef.attach(postContextMenuPortal);
        ref.instance.post = event.data as Post;

        ref.instance.onPostStart = () =>
          this.applicationEndpoint
            .startPosts(event.api.getSelectedRows())
            .subscribe();

        ref.instance.onPostStop = () =>
          this.applicationEndpoint
            .stopPosts(event.api.getSelectedRows())
            .subscribe();

        ref.instance.onPostDelete = () => {
          const dialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
            this.dialog.open<ConfirmComponent, ConfirmDialogData>(
              ConfirmComponent,
              {
                data: {
                  message: `Confirm removal of ${
                    event.api.getSelectedRows().length
                  } post${event.api.getSelectedRows().length > 1 ? 's' : ''}`,
                  confirmCallback: () => {
                    this.applicationEndpoint
                      .deletePosts(event.api.getSelectedRows())
                      .subscribe(() => dialog.close());
                  },
                },
              }
            );
        };

        const subscription = merge(
          fromEvent<MouseEvent>(document, 'click'),
          fromEvent<MouseEvent>(document, 'contextmenu')
        )
          .pipe(take(1))
          .subscribe(() => {
            subscription.unsubscribe();
            postContextMenuOverlayRef?.detach();
            postContextMenuOverlayRef?.dispose();
            ref.destroy();
          });
      },
      onSelectionChanged: (event: SelectionChangedEvent<Post>) => {
        this.selectedChange.emit(event.api.getSelectedRows());
      },
      onRowDoubleClicked: (event: RowDoubleClickedEvent<Post>) => {
        if (!event.data) {
          return;
        }
        const data: ImageDialogData = { postId: event.data.postId };
        this.dialog.open(ImagesComponent, {
          data,
          maxWidth: '100vw',
          maxHeight: '100vh',
          width: '80vw',
          height: '80vh',
        });
      },
    };
  }

  private connect() {
    this.subscriptions.push(
      this.applicationEndpoint.newPosts$.subscribe((newPosts: Post[]) => {
        this.agGrid.api.applyTransaction({ add: newPosts });
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.deletedPosts$.subscribe((e: string[]) => {
        const toRemove: string[] = [];
        e.forEach(v => {
          const rowNode: IRowNode | undefined = this.agGrid.api.getRowNode(v);
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
        });
        this.agGrid.api.applyTransaction({ remove: toRemove });
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.updatedPosts$.subscribe((e: Post[]) => {
        const toUpdate: Post[] = [];
        e.forEach(v => {
          const rowNode: IRowNode | undefined = this.agGrid.api.getRowNode(
            String(v.postId)
          );
          if (rowNode != null) {
            toUpdate.push(v);
          }
        });
        this.agGrid.api.applyTransaction({ update: toUpdate });
      })
    );
  }

  private disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  disableForRows(event: MouseEvent) {
    const target = event.target as HTMLElement;
    const element = document
      .getElementById('download-grid')
      ?.getElementsByClassName('ag-center-cols-container')
      ?.item(0) as HTMLElement;
    if (element.contains(target)) {
      event.preventDefault();
    }
  }
}
