import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererParams } from 'ag-grid-community';
import { AgGridModule, ICellRendererAngularComp } from 'ag-grid-angular';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-progress-cell',
  standalone: true,
  imports: [CommonModule, AgGridModule, MatProgressBarModule],
  templateUrl: './progress-cell.component.html',
  styleUrls: ['./progress-cell.component.scss'],
})
export class ProgressCellComponent implements ICellRendererAngularComp {
  progress = signal(0);
  agInit(params: ICellRendererParams): void {
    this.progress.set(params.value);
  }

  refresh(params: ICellRendererParams): boolean {
    this.progress.set(params.value);
    return true;
  }
}
