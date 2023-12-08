import { Component, Inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgGridAngular, AgGridModule } from 'ag-grid-angular';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import {
  GridOptions,
  ITooltipParams,
  RowDoubleClickedEvent,
} from 'ag-grid-community';
import { ApplicationEndpointService } from '../services/application-endpoint.service';
import { GridReadyEvent } from 'ag-grid-community/dist/lib/events';
import { PostItem } from '../domain/post-item.model';
import { ValueGetterParams } from 'ag-grid-community/dist/lib/entities/colDef';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DialogRef } from '@angular/cdk/dialog';

export interface ThreadDialogData {
  threadId: number;
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
    private dialogRef: DialogRef<ThreadDialogData>,
    private applicationEndpoint: ApplicationEndpointService,
    breakpointObserver: BreakpointObserver
  ) {
    this.gridOptions = <GridOptions>{
      columnDefs: [
        {
          colId: 'number',
          headerName: 'Number',
          field: 'number',
          tooltipField: 'number',
          checkboxSelection: true,
          headerCheckboxSelection: true,
          flex: 1,
        },
        {
          colId: 'title',
          headerName: 'Title',
          field: 'title',
          tooltipField: 'title',
          flex: 2,
        },
        {
          colId: 'url',
          headerName: 'URL',
          field: 'url',
          tooltipField: 'url',
          flex: 2,
        },
        {
          colId: 'hosts',
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
          flex: 1,
        },
      ],
      defaultColDef: {
        resizable: true,
        sortable: true,
      },
      rowSelection: 'multiple',
      getRowId: row => row.data['postId'].toString(),
      onGridReady: (event: GridReadyEvent<PostItem>) => {
        breakpointObserver
          .observe(Breakpoints.HandsetPortrait)
          .subscribe(result => {
            this.agGrid.columnApi.setColumnsVisible(['url'], !result.matches);
            if (result.matches) {
              this.dialogRef.updateSize('100vw', '80vh');
            } else {
              this.dialogRef.updateSize('80vw', '80vh');
            }
            this.dialogRef.updatePosition();
          });
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
