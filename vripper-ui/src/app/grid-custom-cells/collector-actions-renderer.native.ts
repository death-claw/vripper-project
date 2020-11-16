import {GridApi, ICellRendererComp, ICellRendererParams} from 'ag-grid-community';
import {MultiPostService} from '../services/multi-post.service';
import {MultiPostModel} from '../domain/multi-post.model';

export class CollectorActionsRendererNative implements ICellRendererComp {
  private gui: HTMLElement;
  private selectSpan: HTMLSpanElement;
  private selectButton: HTMLButtonElement;
  private deleteSpan: HTMLSpanElement;
  private deleteButton: HTMLButtonElement;
  private multiPostService: MultiPostService;
  private multiPostModel: MultiPostModel;
  private gridApi: GridApi;

  destroy(): void {
    this.selectButton.removeEventListener('click', () => this.multiPostService.selectItems(this.multiPostModel));
    this.deleteButton.removeEventListener('click', () => this.multiPostService.remove(this.gridApi, this.multiPostModel));
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    // @ts-ignore
    this.multiPostService = params.grabQueueService;
    this.multiPostModel = params.node.data;
    this.gridApi = params.api;
    this.gui = document.createElement('div');
    this.gui.setAttribute('style', 'display: flex');
    this.selectSpan = document.createElement('span');
    this.selectButton = document.createElement('button');
    this.selectButton.classList.add('mat-icon-button', 'mat-button-base', 'cell-icon');
    this.selectButton.setAttribute('style', 'width: auto; height: 24px; display: flex; align-items: center');
    this.selectButton.innerHTML = `<svg style="width:24px;height:24px" viewBox="0 0 24 24"><path fill="currentColor" d="M20,2H8A2,2 0 0,0 6,4V16A2,2 0 0,0 8,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H8V4H20V16M16,20V22H4A2,2 0 0,1 2,20V7H4V20H16M18.53,8.06L17.47,7L12.59,11.88L10.47,9.76L9.41,10.82L12.59,14L18.53,8.06Z" /></svg><span>Select</span>`;
    this.selectSpan.append(this.selectButton);

    this.deleteSpan = document.createElement('span');
    this.deleteButton = document.createElement('button');
    this.deleteButton.classList.add('mat-icon-button', 'mat-button-base', 'cell-icon');
    this.deleteButton.setAttribute('style', 'width: auto; height: 24px; display: flex; align-items: center');
    this.deleteButton.innerHTML = `<svg style="width:24px;height:24px" viewBox="0 0 24 24"><path fill="currentColor" d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z" /></svg><span>Remove</span>`;
    this.deleteSpan.append(this.deleteButton);

    this.gui.append(this.selectSpan, this.deleteSpan);

    this.selectButton.addEventListener('click', () => this.multiPostService.selectItems(this.multiPostModel));
    this.deleteButton.addEventListener('click', () => this.multiPostService.remove(this.gridApi, this.multiPostModel));
  }

  refresh(params: ICellRendererParams): boolean {
    this.multiPostModel = params.node.data;
    return true;
  }

}
