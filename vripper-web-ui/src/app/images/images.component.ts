import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import { GridOptions, ValueFormatterParams } from 'ag-grid-community';
import { Subscription } from 'rxjs';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { Image } from '../domain/image.model';
import { MatButtonModule } from '@angular/material/button';
import { ValueGetterParams } from 'ag-grid-community/dist/lib/entities/colDef';
import { ProgressCellComponent } from '../progress-cell/progress-cell.component';
import { ITooltipParams } from 'ag-grid-community/dist/lib/rendering/tooltipComponent';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DialogRef } from '@angular/cdk/dialog';

export interface ImageDialogData {
  postId: number;
}

@Component({
  selector: 'app-images',
  standalone: true,
  imports: [CommonModule, AgGridModule, MatDialogModule, MatButtonModule],
  templateUrl: './images.component.html',
  styleUrls: ['./images.component.scss'],
})
export class ImagesComponent implements OnDestroy {
  @ViewChild('agGrid') agGrid!: AgGridAngular;

  gridOptions: GridOptions<Image>;
  subscriptions: Subscription[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ImageDialogData,
    public dialogRef: DialogRef<ImageDialogData>,
    private applicationEndpoint: ApplicationEndpointService,
    breakpointObserver: BreakpointObserver
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          colId: 'index',
          headerName: '#',
          field: 'index',
          tooltipField: 'index',
          sort: 'asc',
          flex: 1,
        },
        {
          colId: 'url',
          headerName: 'URL',
          field: 'url',
          tooltipField: 'url',
          flex: 2,
        },
        {
          colId: 'progress',
          headerName: 'Progress',
          field: 'progress',
          tooltipValueGetter: (params: ITooltipParams<Image>) => {
            if (!params.data) {
              return 0;
            }
            return params.data.downloaded === 0 || params.data.size === 0
              ? 0
              : (params.data.downloaded / params.data.size) * 100;
          },
          valueGetter: (params: ValueGetterParams<Image>) => {
            if (!params.data) {
              return 0;
            }
            return params.data.downloaded === 0 || params.data.size === 0
              ? 0
              : (params.data.downloaded / params.data.size) * 100;
          },
          cellRenderer: ProgressCellComponent,
          flex: 1,
        },
        {
          colId: 'status',
          headerName: 'Status',
          field: 'status',
          valueFormatter: (params: ValueFormatterParams<Image>) => {
            const value = params.value as string;
            return (
              value.at(0)?.toUpperCase() +
              value.substring(1, value.length).toLowerCase()
            );
          },
          flex: 1,
        },
      ],
      defaultColDef: {
        sortable: true,
        resizable: true,
      },
      getRowId: row => row.data['url'].toString(),
      onGridReady: () => {
        this.connect();
        breakpointObserver
          .observe(Breakpoints.HandsetPortrait)
          .subscribe(result => {
            this.agGrid.columnApi.setColumnsVisible(
              ['index', 'status'],
              !result.matches
            );
            if (result.matches) {
              this.dialogRef.updateSize('100vw', '80vh');
            } else {
              this.dialogRef.updateSize('80vw', '80vh');
            }
            this.dialogRef.updatePosition();
          });
      },
    };
  }

  private connect() {
    this.subscriptions.push(
      this.applicationEndpoint
        .postDetails$(this.data.postId)
        .subscribe((e: Image[]) => {
          const toAdd: Image[] = [];
          const toUpdate: Image[] = [];
          e.forEach(v => {
            if (this.agGrid.api.getRowNode(v.url.toString()) == null) {
              toAdd.push(v);
            } else {
              toUpdate.push(v);
            }
          });
          this.agGrid.api.applyTransaction({ update: toUpdate, add: toAdd });
        })
    );
  }

  private disconnect() {
    this.subscriptions.forEach(e => e.unsubscribe());
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
