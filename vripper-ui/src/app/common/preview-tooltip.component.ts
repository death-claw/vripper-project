import { Component, Input } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-preview',
  template: `
  <div class="previews" style="display: inline-block; white-space: nowrap; height: 200px; max-width: 400px">
    <ng-container *ngFor="let link of links">
      <img
        [@simpleFadeAnimation]="'in'"
        [src]="link"
      />
    </ng-container>
  </div>
  `,
  styles: [
    `
      img {
        max-height: 200px;
        max-width: 400px;
        margin-left: 5px;
      }
    `
  ],
  animations: [
    trigger('simpleFadeAnimation', [
      state('in', style({ opacity: 1 })),

      transition(':enter', [style({ opacity: 0 }), animate(300)])
    ])
  ]
})
export class AppPreviewComponent {
  @Input() links: string[] = [];
}
