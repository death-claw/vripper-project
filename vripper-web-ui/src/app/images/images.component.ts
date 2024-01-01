import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { Image } from '../domain/image.model';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DialogRef } from '@angular/cdk/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { isDisplayed, progress, statusIcon } from '../utils/utils';
import { DataSource, SelectionModel } from '@angular/cdk/collections';
import { ImageRow } from '../domain/image-row.model';

export interface ImageDialogData {
  postId: number;
}

@Component({
  selector: 'app-images',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
  ],
  templateUrl: './images.component.html',
  styleUrls: ['./images.component.scss'],
})
export class ImagesComponent {
  displayedColumns: string[] = ['index', 'url', 'progress', 'status'];
  selection = new SelectionModel<ImageRow>(
    false,
    [],
    true,
    (a, b) => a.url === b.url
  );
  dataSource = new ImageDataSource(this.applicationEndpoint, this.data.postId);

  columnsToDisplay = signal([...this.displayedColumns]);
  isDisplayed = isDisplayed;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ImageDialogData,
    public dialogRef: DialogRef<ImageDialogData>,
    private applicationEndpoint: ApplicationEndpointService,
    breakpointObserver: BreakpointObserver
  ) {
    breakpointObserver
      .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium])
      .subscribe(result => {
        if (result.matches) {
          this.columnsToDisplay.set(['index', 'progress', 'status']);
        } else {
          this.columnsToDisplay.set(['index', 'url', 'progress', 'status']);
        }
        if (result.matches) {
          this.dialogRef.updateSize('100vw', '80vh');
        } else {
          this.dialogRef.updateSize('80vw', '80vh');
        }
        this.dialogRef.updatePosition();
      });
  }

  onClick(row: ImageRow) {
    this.selection.clear();
    this.selection.select(row);
  }
}

class ImageDataSource extends DataSource<ImageRow> {
  subscriptions: Subscription[] = [];
  _dataStream = new BehaviorSubject<ImageRow[]>([]);

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private postId: number
  ) {
    super();
  }

  connect(): Observable<ImageRow[]> {
    this.subscriptions.push(
      this.applicationEndpoint
        .postDetails$(this.postId)
        .subscribe((e: Image[]) => {
          e.forEach(image => {
            const rowNode = this._dataStream.value.find(
              d => d.url === image.url
            );
            if (rowNode == null) {
              this._dataStream.next([
                ...this._dataStream.value,
                new ImageRow(
                  image.postId,
                  image.url,
                  image.status,
                  image.index,
                  image.downloaded,
                  image.size
                ),
              ]);
            } else {
              Object.assign(rowNode, image);
              rowNode.statusIcon.set(statusIcon(image.status));
              rowNode.progress.set(progress(image.downloaded, image.size));
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
