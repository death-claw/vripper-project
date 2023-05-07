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

export interface ImageDialogData {
  postId: string;
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

  gridOptions: GridOptions;
  subscriptions: Subscription[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ImageDialogData,
    private applicationEndpoint: ApplicationEndpointService
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: '#',
          field: 'index',
          tooltipField: 'index',
          sort: 'asc',
        },
        {
          headerName: 'URL',
          field: 'url',
          tooltipField: 'url',
          flex: 1,
        },
        {
          headerName: 'Progress',
          field: 'progress',
          tooltipValueGetter: params => {
            if (!params.data) {
              return 0;
            }
            return params.data.current === 0 || params.data.total === 0
              ? 0
              : (params.data.current / params.data.total) * 100;
          },
          valueGetter: (params: ValueGetterParams<Image>) => {
            if (!params.data) {
              return 0;
            }
            return params.data.current === 0 || params.data.total === 0
              ? 0
              : (params.data.current / params.data.total) * 100;
          },
          cellRenderer: ProgressCellComponent,
        },
        {
          headerName: 'Status',
          field: 'status',
          valueFormatter: (params: ValueFormatterParams<Image>) => {
            const value = params.value as string;
            return (
              value.at(0)?.toUpperCase() +
              value.substring(1, value.length).toLowerCase()
            );
          },
        },
      ],
      defaultColDef: {
        sortable: true,
        resizable: true,
      },
      getRowId: row => row.data['url'].toString(),
      onGridReady: () => this.connect(),
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
