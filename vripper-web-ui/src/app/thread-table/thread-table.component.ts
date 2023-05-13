import {
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
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
} from 'ag-grid-community';
import { filter, fromEvent, merge, Observable, Subscription, take } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { Thread } from '../domain/thread.model';
import { ComponentPortal, PortalModule } from '@angular/cdk/portal';
import {
  Overlay,
  OverlayModule,
  OverlayPositionBuilder,
} from '@angular/cdk/overlay';
import { ThreadContextmenuComponent } from '../thread-contextmenu/thread-contextmenu.component';
import {
  ThreadDialogData,
  ThreadSelectionComponent,
} from '../thread-selection/thread-selection.component';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {
  ConfirmComponent,
  ConfirmDialogData,
} from '../confirm/confirm.component';

@Component({
  selector: 'app-thread-table',
  standalone: true,
  imports: [
    CommonModule,
    AgGridModule,
    OverlayModule,
    PortalModule,
    MatDialogModule,
  ],
  templateUrl: './thread-table.component.html',
  styleUrls: ['./thread-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThreadTableComponent implements OnInit, OnDestroy {
  @ViewChild('agGrid') agGrid!: AgGridAngular;

  @Input({ required: true })
  clear!: Observable<void>;

  @Output()
  rowCountChange = new EventEmitter<number>();

  gridOptions: GridOptions;
  subscriptions: Subscription[] = [];

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private overlayPositionBuilder: OverlayPositionBuilder,
    private overlay: Overlay,
    private dialog: MatDialog
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Url',
          field: 'link',
          tooltipField: 'link',
          flex: 1,
        },
        {
          headerName: 'Count',
          field: 'total',
          tooltipField: 'total',
        },
      ],
      defaultColDef: {
        sortable: true,
        resizable: true,
      },
      rowSelection: 'multiple',
      getRowId: row => row.data['threadId'],
      onGridReady: () => this.connect(),
      onRowDataUpdated: (event: RowDataUpdatedEvent) =>
        this.rowCountChange.emit(event.api.getDisplayedRowCount()),
      onCellContextMenu: (event: CellContextMenuEvent<Thread>) => {
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
          ThreadContextmenuComponent
        );
        const ref: ComponentRef<ThreadContextmenuComponent> =
          postContextMenuOverlayRef.attach(postContextMenuPortal);
        ref.instance.thread = event.data as Thread;

        ref.instance.onThreadSelection = () => {
          if (!event.data) {
            return;
          }
          const data: ThreadDialogData = { threadId: event.data.threadId };
          this.dialog.open(ThreadSelectionComponent, { data });
        };

        ref.instance.onThreadDelete = () => {
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
                      .deleteThreads(event.api.getSelectedRows())
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
      onRowDoubleClicked: (event: RowDoubleClickedEvent<Thread>) => {
        if (!event.data) {
          return;
        }
        const data: ThreadDialogData = { threadId: event.data.threadId };
        this.dialog.open(ThreadSelectionComponent, { data });
      },
    };
  }

  ngOnInit(): void {
    this.clear.subscribe(() => this.agGrid.api.setRowData([]));
  }

  private connect() {
    this.subscriptions.push(
      this.applicationEndpoint.threads$.subscribe((e: Thread[]) => {
        const toAdd: Thread[] = [];
        const toUpdate: Thread[] = [];
        e.forEach(v => {
          if (this.agGrid.api.getRowNode(v.threadId) == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.agGrid.api.applyTransaction({ update: toUpdate, add: toAdd });
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.threadRemove$.subscribe((e: string[]) => {
        const toRemove: any[] = [];
        e.forEach(v => {
          const rowNode: IRowNode | undefined = this.agGrid.api.getRowNode(v);
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
          return;
        });
        this.agGrid.api.applyTransaction({ remove: toRemove });
      })
    );
  }

  private disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }

  disableForRows(event: MouseEvent) {
    const target = event.target as HTMLElement;
    const element = document
      .getElementById('thread-grid')
      ?.getElementsByClassName('ag-center-cols-container')
      ?.item(0) as HTMLElement;
    if (element.contains(target)) {
      event.preventDefault();
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
