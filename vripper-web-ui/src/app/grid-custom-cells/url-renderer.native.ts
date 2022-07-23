import {ICellRendererComp, ICellRendererParams} from 'ag-grid-community';

export class UrlRendererNative implements ICellRendererComp {

  private gui!: HTMLElement;
  private text!: HTMLSpanElement;
  private link!: HTMLAnchorElement;
  private url!: string;

  destroy(): void {
    if (this.link) {
      this.link.removeEventListener('click', this.goTo.bind(this));
    }
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    this.url = params.value;
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');
    this.text = document.createElement('span');
    this.link = document.createElement('a');
    this.link.setAttribute('href', 'javascript:void(0)');
    this.link.innerText = this.url;

    this.text.append(this.link);
    this.gui.append(this.text);
    this.link.addEventListener('click', this.goTo.bind(this));
  }

  refresh(params: ICellRendererParams): boolean {
    this.url = params.value;
    this.link.innerText = this.url;
    return true;
  }

  goTo() {
    window.open(this.url, '_blank');
  }
}
