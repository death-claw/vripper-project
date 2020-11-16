import {ICellRendererComp, ICellRendererParams} from 'ag-grid-community';

export class ProgressRendererNative implements ICellRendererComp {

  protected gui: HTMLElement;
  protected progressBackDiv: HTMLElement;
  protected progressFrontDiv: HTMLElement;
  private progress: number;

  destroy(): void {
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    this.progress = params.value;
    this.gui = document.createElement('div');
    this.gui.setAttribute('class', 'native-progress-bar-container');
    this.progressBackDiv = document.createElement('div');
    this.progressBackDiv.setAttribute('class', 'native-progress-bar-back');
    this.progressFrontDiv = document.createElement('div');
    this.progressFrontDiv.setAttribute('class', 'native-progress-bar-front');
    this.progressFrontDiv.setAttribute('style', `width: ${this.progress}%`);
    this.progressBackDiv.append(this.progressFrontDiv);
    this.gui.append(this.progressBackDiv);
  }

  refresh(params: ICellRendererParams): boolean {
    this.progress = params.value;
    this.progressFrontDiv.setAttribute('style', `width: ${this.progress}%`);
    return true;
  }

}
