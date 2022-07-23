import {ChangeDetectionStrategy, Component} from '@angular/core';
import {environment} from '../../environments/environment';

@Component({
  selector: 'app-about',
  templateUrl: './about-component.html',
  styleUrls: ['./about-component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AboutComponent {

  version = environment.version;

  goTo(url: string) {
    window.open(url, '_blank');
  }
}
