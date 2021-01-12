import {GridApi, ICellRendererComp, ICellRendererParams, RowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';

export class PostFilesRendererNative implements ICellRendererComp {

  private gui: HTMLElement;
  private done: number;
  private total: number;
  private hosts: string[];
  private text: HTMLElement;
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
    this.done = params.node.data.done;
    this.total = params.node.data.total;
    this.hosts = params.node.data.hosts;
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');
    this.text = document.createElement('span');
    this.text.innerText = `${this.done}/${this.total} from ${this.hosts.join(', ')}`;
    this.text.setAttribute('title', `${this.done}/${this.total} from ${this.hosts.join(', ')}`);
    this.gui.append(this.text);
    this.gui.addEventListener('contextmenu', this.context.bind(this));
  }

  refresh(params: ICellRendererParams): boolean {
    this.done = params.node.data.done;
    this.total = params.node.data.total;
    this.hosts = params.node.data.hosts;
    this.text.innerText = `${this.done}/${this.total} from ${this.hosts.join(', ')}`;
    this.text.setAttribute('title', `${this.done}/${this.total} from ${this.hosts.join(', ')}`);
    return true;
  }

  context(event: MouseEvent) {
    event.preventDefault();
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostContextMenu(event, this.node.data);
  }
}
