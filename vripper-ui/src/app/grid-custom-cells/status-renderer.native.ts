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
      case 'PROCESSING':
        return `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M15.9,18.45C17.25,18.45 18.35,17.35 18.35,16C18.35,14.65 17.25,13.55 15.9,13.55C14.54,13.55 13.45,14.65 13.45,16C13.45,17.35 14.54,18.45 15.9,18.45M21.1,16.68L22.58,17.84C22.71,17.95 22.75,18.13 22.66,18.29L21.26,20.71C21.17,20.86 21,20.92 20.83,20.86L19.09,20.16C18.73,20.44 18.33,20.67 17.91,20.85L17.64,22.7C17.62,22.87 17.47,23 17.3,23H14.5C14.32,23 14.18,22.87 14.15,22.7L13.89,20.85C13.46,20.67 13.07,20.44 12.71,20.16L10.96,20.86C10.81,20.92 10.62,20.86 10.54,20.71L9.14,18.29C9.05,18.13 9.09,17.95 9.22,17.84L10.7,16.68L10.65,16L10.7,15.31L9.22,14.16C9.09,14.05 9.05,13.86 9.14,13.71L10.54,11.29C10.62,11.13 10.81,11.07 10.96,11.13L12.71,11.84C13.07,11.56 13.46,11.32 13.89,11.15L14.15,9.29C14.18,9.13 14.32,9 14.5,9H17.3C17.47,9 17.62,9.13 17.64,9.29L17.91,11.15C18.33,11.32 18.73,11.56 19.09,11.84L20.83,11.13C21,11.07 21.17,11.13 21.26,11.29L22.66,13.71C22.75,13.86 22.71,14.05 22.58,14.16L21.1,15.31L21.15,16L21.1,16.68M6.69,8.07C7.56,8.07 8.26,7.37 8.26,6.5C8.26,5.63 7.56,4.92 6.69,4.92A1.58,1.58 0 0,0 5.11,6.5C5.11,7.37 5.82,8.07 6.69,8.07M10.03,6.94L11,7.68C11.07,7.75 11.09,7.87 11.03,7.97L10.13,9.53C10.08,9.63 9.96,9.67 9.86,9.63L8.74,9.18L8,9.62L7.81,10.81C7.79,10.92 7.7,11 7.59,11H5.79C5.67,11 5.58,10.92 5.56,10.81L5.4,9.62L4.64,9.18L3.5,9.63C3.41,9.67 3.3,9.63 3.24,9.53L2.34,7.97C2.28,7.87 2.31,7.75 2.39,7.68L3.34,6.94L3.31,6.5L3.34,6.06L2.39,5.32C2.31,5.25 2.28,5.13 2.34,5.03L3.24,3.47C3.3,3.37 3.41,3.33 3.5,3.37L4.63,3.82L5.4,3.38L5.56,2.19C5.58,2.08 5.67,2 5.79,2H7.59C7.7,2 7.79,2.08 7.81,2.19L8,3.38L8.74,3.82L9.86,3.37C9.96,3.33 10.08,3.37 10.13,3.47L11.03,5.03C11.09,5.13 11.07,5.25 11,5.32L10.03,6.06L10.06,6.5L10.03,6.94Z" />
</svg>`;
      case 'COMPLETE':
      case 'DONE':
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
      case 'PROCESSING':
        return 'downloading';
      case 'COMPLETE':
      case 'DONE':
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
