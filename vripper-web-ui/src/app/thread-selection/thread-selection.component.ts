import { Component, Inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {
  GridOptions,
  ITooltipParams,
  RowDoubleClickedEvent,
} from 'ag-grid-community';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { GridReadyEvent } from 'ag-grid-community/dist/lib/events';
import { PostItem } from '../domain/post-item.model';
import { ValueGetterParams } from 'ag-grid-community/dist/lib/entities/colDef';

export interface ThreadDialogData {
  threadId: string;
}

@Component({
  selector: 'app-thread-selection',
  standalone: true,
  imports: [CommonModule, AgGridModule, MatButtonModule, MatDialogModule],
  templateUrl: './thread-selection.component.html',
  styleUrls: ['./thread-selection.component.scss'],
})
export class ThreadSelectionComponent {
  @ViewChild('agGrid') agGrid!: AgGridAngular<PostItem>;

  gridOptions: GridOptions;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ThreadDialogData,
    private dialogRef: MatDialogRef<ThreadSelectionComponent>,
    private applicationEndpoint: ApplicationEndpointService
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          headerName: 'Number',
          field: 'number',
          tooltipField: 'number',
          checkboxSelection: true,
          headerCheckboxSelection: true,
        },
        {
          headerName: 'Title',
          field: 'title',
          tooltipField: 'title',
          flex: 2,
        },
        {
          headerName: 'URL',
          field: 'url',
          tooltipField: 'url',
          flex: 1,
        },
        {
          headerName: 'Hosts',
          field: 'hosts',
          tooltipValueGetter: (params: ITooltipParams<PostItem>) => {
            return params.data?.hosts
              .map(v => `${v.first} (${v.second})`)
              .join(', ');
          },
          valueGetter: (params: ValueGetterParams<PostItem>) => {
            return params.data?.hosts
              .map(v => `${v.first} (${v.second})`)
              .join(', ');
          },
        },
      ],
      defaultColDef: {
        resizable: true,
        sortable: true,
      },
      rowSelection: 'multiple',
      getRowId: row => row.data['postId'].toString(),
      onGridReady: (event: GridReadyEvent<PostItem>) => {
        this.applicationEndpoint
          .getThreadPosts(this.data.threadId)
          .subscribe(result => {
            event.api.applyTransaction({ add: result });
          });
      },
      onRowDoubleClicked: (event: RowDoubleClickedEvent<PostItem>) => {
        if (!event.data) {
          return;
        }
        this.download([event.data]);
      },
    };
  }

  downloadSelected() {
    this.download(this.agGrid.api.getSelectedRows());
  }

  private download = (items: PostItem[]) => {
    this.applicationEndpoint.download(items).subscribe(() => {
      this.dialogRef.close();
    });
  };
}
