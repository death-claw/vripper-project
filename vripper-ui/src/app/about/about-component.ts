import {ChangeDetectionStrategy, Component} from '@angular/core';
import {environment} from '../../environments/environment';
import {ElectronService} from 'ngx-electron';

@Component({
  selector: 'app-about',
  templateUrl: './about-component.html',
  styleUrls: ['./about-component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AboutComponent {

  version = environment.version;

  constructor(
    public electronService: ElectronService
  ) {
  }

  goTo(url: string) {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(url);
    } else {
      window.open(url, '_blank');
    }
  }
}
