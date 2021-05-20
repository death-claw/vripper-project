import {GridApi, ICellRendererComp, ICellRendererParams, RowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';

export class PostOrderRendererNative implements ICellRendererComp {
  private gui: HTMLElement;
  private order: number;
  private text: HTMLSpanElement;
  private gridApi: GridApi;
  private node: RowNode;
  private contextMenuService: PostContextMenuService;

  destroy(): void {
    if (this.gui) {
      this.gui.removeEventListener('contextmenu', this.context.bind(this));
    }
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    // @ts-ignore
    this.contextMenuService = params.contextMenuService;
    this.gridApi = params.api;
    this.node = params.node;
    this.order = params.node.data.rank;
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');
    this.text = document.createElement('span');
    this.text.innerText = this.order.toString(10);
    this.text.setAttribute('title', this.order.toString(10));
    this.gui.append(this.text);
    this.gui.addEventListener('contextmenu', this.context.bind(this));
  }

  refresh(params: ICellRendererParams): boolean {
    this.order = params.node.data.rank;
    this.text.innerText = this.order.toString(10);
    this.text.setAttribute('title', this.order.toString(10));
    return true;
  }

  context(event: MouseEvent) {
    event.preventDefault();
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostContextMenu(event, this.node.data);
  }
}
