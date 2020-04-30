import {ChangeDetectionStrategy, Component, OnDestroy, OnInit} from '@angular/core';
import {AgRendererComponent} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {VRPostParse} from '../../domain/vr-post-parse.model';
import {ElectronService} from 'ngx-electron';

@Component({
  selector: 'app-url-cell',
  templateUrl: 'url-renderer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UrlRendererComponent implements OnInit, OnDestroy, AgRendererComponent {
  constructor(public electronService: ElectronService) {
  }

  postResult: VRPostParse;
  params: ICellRendererParams;

  ngOnInit(): void {}

  ngOnDestroy(): void {}

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postResult = this.params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    return false;
  }

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postResult.url);
    } else {
      window.open(this.postResult.url, '_blank');
    }
  }
}
