import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { DialogRef } from '@angular/cdk/dialog';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, finalize, Observable } from 'rxjs';
import { PostItem } from '../domain/post-item.model';
import { Thread } from '../domain/thread.model';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { isDisplayed } from '../utils/utils';

export interface ThreadDialogData {
  threadId: number;
}

@Component({
  selector: 'app-thread-selection',
  imports: [
    CommonModule,
    MatButtonModule,
    MatDialogModule,
    MatCheckboxModule,
    MatTableModule,
  ],
  templateUrl: './thread-selection.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class ThreadSelectionComponent {
  dataSource = new ThreadSelectionDataSource(
    this.applicationEndpoint,
    this.data.threadId
  );
  displayedColumns: string[] = ['number', 'title', 'url', 'hosts'];
  selection = new SelectionModel<PostItem>(
    true,
    [],
    true,
    (a, b) => a.url === b.url
  );
  isDisplayed = isDisplayed;
  selectedChange = new EventEmitter<Thread[]>();
  columnsToDisplay = signal([...this.displayedColumns]);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ThreadDialogData,
    private dialogRef: DialogRef<ThreadDialogData>,
    private applicationEndpoint: ApplicationEndpointService
  ) {}

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
  checkboxLabel(row?: PostItem): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${
      row.url
    }`;
  }

  onClick(row: PostItem, event: MouseEvent) {
    if (!event.ctrlKey) {
      this.selection.clear();
    }
    this.selection.select(row);
  }

  onRowDoubleClicked(row: PostItem) {
    this.download([row]);
  }

  downloadSelected() {
    this.download(this.selection.selected);
  }

  private download = (items: PostItem[]) => {
    this.applicationEndpoint.download(items).subscribe(() => {
      this.dialogRef.close();
    });
  };
}

class ThreadSelectionDataSource extends DataSource<PostItem> {
  _dataStream = new BehaviorSubject<PostItem[]>([]);
  loading = signal(true);

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private threadId: number
  ) {
    super();
  }

  connect(): Observable<PostItem[]> {
    this.applicationEndpoint
      .getThreadPosts(this.threadId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => {
        this._dataStream.next(result);
      });
    return this._dataStream.asObservable();
  }

  disconnect(): void {}
}
