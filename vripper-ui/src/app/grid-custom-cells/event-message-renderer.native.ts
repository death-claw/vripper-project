import {GridApi, ICellRendererComp, ICellRendererParams} from 'ag-grid-community';
import {EventLog} from '../domain/event.model';
import {EventLogService} from '../services/event-log.service';

export class EventMessageRendererNative implements ICellRendererComp {
  private gui: HTMLElement;
  private viewSpan: HTMLSpanElement;
  private viewButton: HTMLButtonElement;
  private eventLogService: EventLogService;
  private eventLog: EventLog;
  private gridApi: GridApi;
  private text: HTMLSpanElement;

  destroy(): void {
    this.viewButton.removeEventListener('click', () => this.eventLogService.openDialog(this.eventLog.message));
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    // @ts-ignore
    this.eventLogService = params.eventLogService;
    this.eventLog = params.node.data;
    this.gridApi = params.api;
    this.gui = document.createElement('div');
    this.gui.setAttribute('style', 'display: flex; justify-content: space-between;');
    this.text = document.createElement('span');
    this.text.classList.add('no-wrap');
    this.text.textContent = this.eventLog.message;
    this.viewSpan = document.createElement('span');
    this.viewButton = document.createElement('button');
    this.viewButton.classList.add('mat-icon-button', 'mat-button-base', 'cell-icon');
    this.viewButton.setAttribute('style', 'width: auto; height: 24px; display: flex; align-items: center');
    this.viewButton.innerHTML = `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4.5C7,4.5 2.73,7.61 1,12C2.73,16.39 7,19.5 12,19.5C17,19.5 21.27,16.39 23,12C21.27,7.61 17,4.5 12,4.5Z" /></svg><span style="margin-left: 5px">View</span>`;
    this.viewSpan.append(this.viewButton);

    this.gui.append(this.text, this.viewSpan);

    this.viewButton.addEventListener('click', () => this.eventLogService.openDialog(this.eventLog.message));
  }

  refresh(params: ICellRendererParams): boolean {
    this.eventLog = params.node.data;
    this.text.textContent = this.eventLog.message;
    return true;
  }
}
