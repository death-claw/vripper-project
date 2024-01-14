import {
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  EventEmitter,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Post } from '../domain/post.model';
import {
  BehaviorSubject,
  EMPTY,
  mergeMap,
  Observable,
  Subscription,
  take,
} from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import {
  Overlay,
  OverlayModule,
  OverlayPositionBuilder,
} from '@angular/cdk/overlay';
import { ComponentPortal, PortalModule } from '@angular/cdk/portal';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatTableModule } from '@angular/material/table';
import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  isDisplayed,
  progress,
  statusIcon,
  totalFormatter,
} from '../utils/utils';
import { ImageDialogData, ImagesComponent } from '../images/images.component';
import { PostContextmenuComponent } from '../post-contextmenu/post-contextmenu.component';
import {
  ConfirmComponent,
  ConfirmDialogData,
} from '../confirm/confirm.component';
import { PostRow } from '../domain/post-row.model';
import {
  RenameDialogComponent,
  RenameDialogData,
  RenameDialogResult,
} from '../rename-dialog/rename-dialog.component';

@Component({
  selector: 'app-download-table',
  standalone: true,
  imports: [
    CommonModule,
    OverlayModule,
    PortalModule,
    MatDialogModule,
    MatListModule,
    MatTableModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './download-table.component.html',
  styleUrls: ['./download-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadTableComponent {
  dataSource = new PostDataSource(this.applicationEndpoint);

  @Output()
  rowCountChange = new EventEmitter<number>();

  @Output()
  selectedPostsChange = new EventEmitter<Post[]>();
  @Output()
  selectedPostChange = new EventEmitter<Post>();

  displayedColumns: string[] = [
    'title',
    'progress',
    'status',
    'path',
    'total',
    'hosts',
    'addedOn',
    'order',
  ];
  selection = new SelectionModel<PostRow>(
    true,
    [],
    true,
    (a, b) => a.postId === b.postId
  );

  isDisplayed = isDisplayed;

  columnsToDisplay = signal([...this.displayedColumns]);

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private overlayPositionBuilder: OverlayPositionBuilder,
    private overlay: Overlay,
    private dialog: MatDialog,
    private breakpointObserver: BreakpointObserver
  ) {
    this.dataSource._dataStream.subscribe(v =>
      this.rowCountChange.emit(v.length)
    );
    this.selection.changed.subscribe(selectionChange => {
      this.selectedPostChange.emit(selectionChange.added[0]);
      this.selectedPostsChange.emit(this.selection.selected);
    });
    this.breakpointObserver
      .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium])
      .subscribe(result => {
        if (result.matches) {
          this.columnsToDisplay.set(['title', 'progress', 'status']);
        } else {
          this.columnsToDisplay.set([
            'title',
            'progress',
            'status',
            'path',
            'total',
            'hosts',
            'addedOn',
            'order',
          ]);
        }
      });
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource._dataStream.value.length;
    return numSelected === numRows;
  }

  /** Selects all rows if they are not all selected; otherwise clear selection. */
  toggleAllRows() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }

    this.selection.select(...this.dataSource._dataStream.value);
  }

  /** The label for the checkbox on the passed row */
  checkboxLabel(row?: PostRow): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${
      row.postId
    }`;
  }

  onRowDoubleClicked(event: PostRow) {
    const data: ImageDialogData = { postId: event.postId };
    this.dialog.open(ImagesComponent, {
      data,
      maxWidth: '100vw',
      maxHeight: '100vh',
      width: '80vw',
      height: '80vh',
    });
  }

  onClick(row: PostRow, $event: MouseEvent) {
    if (!$event.ctrlKey) {
      this.selection.clear();
    }
    this.selection.select(row);
  }

  onContextMenu(mouseEvent: MouseEvent, row: PostRow) {
    mouseEvent.preventDefault();
    if (this.selection.selected.length > 1) {
      this.selection.select(row);
    } else {
      this.selection.clear();
      this.selection.select(row);
    }
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
    const postContextMenuPortal = new ComponentPortal(PostContextmenuComponent);
    const ref: ComponentRef<PostContextmenuComponent> =
      postContextMenuOverlayRef.attach(postContextMenuPortal);
    ref.instance.post = row as PostRow;

    ref.instance.close = () => {
      postContextMenuOverlayRef?.detach();
      postContextMenuOverlayRef?.dispose();
      ref.destroy();
    };

    ref.instance.onPostStart = () =>
      this.applicationEndpoint.startPosts(this.selection.selected).subscribe();

    ref.instance.onPostStop = () =>
      this.applicationEndpoint.stopPosts(this.selection.selected).subscribe();

    ref.instance.onPostRename = () => {
      const dialog: MatDialogRef<RenameDialogComponent, RenameDialogResult> =
        this.dialog.open<
          RenameDialogComponent,
          RenameDialogData,
          RenameDialogResult
        >(RenameDialogComponent, {
          data: { postId: row.postId, name: row.folderName },
        });
      dialog
        .afterClosed()
        .pipe(
          mergeMap(result => {
            if (result) {
              return this.applicationEndpoint.renamePost(result);
            } else {
              return EMPTY;
            }
          })
        )
        .subscribe();
    };

    ref.instance.onPostDelete = () => {
      const dialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
        this.dialog.open<ConfirmComponent, ConfirmDialogData>(
          ConfirmComponent,
          {
            data: {
              message: `Confirm removal of ${
                this.selection.selected.length
              } post${this.selection.selected.length > 1 ? 's' : ''}`,
              confirmCallback: () => {
                this.applicationEndpoint
                  .deletePosts(this.selection.selected)
                  .subscribe(() => dialog.close());
              },
            },
          }
        );
    };

    postContextMenuOverlayRef
      .outsidePointerEvents()
      .pipe(take(1))
      .subscribe(() => {
        console.log('click away');
        postContextMenuOverlayRef?.detach();
        postContextMenuOverlayRef?.dispose();
        ref.destroy();
      });
  }
}

class PostDataSource extends DataSource<PostRow> {
  subscriptions: Subscription[] = [];
  _dataStream = new BehaviorSubject<PostRow[]>([]);

  constructor(private applicationEndpoint: ApplicationEndpointService) {
    super();
  }

  connect(): Observable<PostRow[]> {
    this.subscriptions.push(
      this.applicationEndpoint.newPosts$.subscribe((newPosts: Post[]) => {
        this._dataStream.next([
          ...this._dataStream.value,
          ...newPosts.map(
            e =>
              new PostRow(
                e.postId,
                e.postTitle,
                e.status,
                e.url,
                e.done,
                e.total,
                e.hosts,
                e.addedOn,
                e.rank,
                e.downloadDirectory,
                e.folderName,
                e.downloadFolder,
                e.downloaded
              )
          ),
        ]);
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.deletedPosts$.subscribe((e: number[]) => {
        this._dataStream.next([
          ...this._dataStream.value.filter(
            v => e.find(d => d === v.postId) == null
          ),
        ]);
      })
    );

    this.subscriptions.push(
      this.applicationEndpoint.updatedPosts$.subscribe((e: Post[]) => {
        e.forEach(v => {
          const rowNode = this._dataStream.value.find(
            d => d.postId === v.postId
          );
          if (rowNode != null) {
            Object.assign(rowNode, v);
            rowNode.statusIcon.set(statusIcon(v.status));
            rowNode.progress.set(progress(v.done, v.total));
            rowNode.total2.set(totalFormatter(v.done, v.total, v.downloaded));
            rowNode.path.set(v.downloadFolder);
          }
        });
      })
    );
    return this._dataStream.asObservable();
  }

  disconnect(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
