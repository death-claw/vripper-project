import {ICellRendererComp, ICellRendererParams} from 'ag-grid-community';

export class StatusRendererNative implements ICellRendererComp {

  protected gui: HTMLElement;
  private container: HTMLElement;
  private icon: HTMLElement;
  private text: HTMLElement;
  private status: string;
  private currentStatusClass;

  destroy(): void {
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    this.status = params.value;
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');

    this.container = document.createElement('span');
    this.container.classList.add('native-status-container');

    this.icon = document.createElement('span');
    this.currentStatusClass = this.getClass(this.status);
    this.icon.classList.add('native-status-icon', this.currentStatusClass);
    this.icon.innerHTML = this.getIcon(this.status);

    this.text = document.createElement('span');
    this.text.classList.add('native-status-text');
    this.text.innerText = this.titleCase(this.status);

    this.container.append(this.icon, this.text);
    this.gui.append(this.container);
  }

  refresh(params: any): boolean {
    const oldStatusClass = this.currentStatusClass;
    this.status = params.value;
    this.currentStatusClass = this.getClass(this.status);
    this.icon.classList.replace(oldStatusClass, this.currentStatusClass);
    this.icon.innerHTML = this.getIcon(this.status);

    this.text.innerText = this.titleCase(this.status);
    return true;
  }

  titleCase(string: string): string {
    return string[0].toUpperCase() + string.substr(1, string.length).toLowerCase();
  }

  getIcon(status: string) {
    switch (status) {
      case 'PENDING':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z" />
</svg>`;
      case 'DOWNLOADING':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M5,20H19V18H5M19,9H15V3H9V9H5L12,16L19,9Z" />
</svg>`;
      case 'COMPLETE':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
</svg>`;
      case 'ERROR':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M12,2L1,21H23M12,6L19.53,19H4.47M11,10V14H13V10M11,16V18H13V16" />
</svg>`;
      case 'STOPPED':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
            <path fill="currentColor" d="M14,19H18V5H14M6,19H10V5H6V19Z" />
        </svg></span>`;
      case 'PARTIAL':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M5,20H19V18H5M19,9H15V3H9V9H5L12,16L19,9Z" />
</svg>`;
    }
  }

  getClass(status: string) {
    switch (status) {
      case 'PENDING':
        return 'pending';
      case 'DOWNLOADING':
        return 'downloading';
      case 'COMPLETE':
        return 'complete';
      case 'ERROR':
        return 'error';
      case 'STOPPED':
        return 'stopped';
      case 'PARTIAL':
        return 'downloading';
    }
  }
}
