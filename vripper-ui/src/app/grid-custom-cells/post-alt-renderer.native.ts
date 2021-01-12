import {GridApi, ICellRendererComp, ICellRendererParams, RowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';

export class PostAltRendererNative implements ICellRendererComp {
  private gui: HTMLElement;
  private names: string[];
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
    this.names = params.node.data.metadata?.resolvedNames || [];
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');
    this.text = document.createElement('span');
    this.text.innerText = this.names.join(', ') || 'none';
    this.text.setAttribute('title', this.names.join(', ') || 'none');
    this.gui.append(this.text);
    this.gui.addEventListener('contextmenu', this.context.bind(this));
  }

  refresh(params: ICellRendererParams): boolean {
    this.names = params.node.data.metadata?.resolvedNames || [];
    this.text.innerText = this.names.join(', ') || 'none';
    this.text.setAttribute('title', this.names.join(', ') || 'none');
    return true;
  }

  context(event: MouseEvent) {
    event.preventDefault();
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostContextMenu(event, this.node.data);
  }
}
