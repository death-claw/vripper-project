import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-preview',
  template: `
    <div class="previews" style="display: inline-block; white-space: nowrap;">
      <ng-container *ngFor="let link of links">
        <img
          [src]="link"
          (load)="loaded($event)"
        />
      </ng-container>
    </div>
  `,
  styles: [
    `
      img {
        height: 150px;
        width: 150px;
        margin-left: 5px;
        opacity: 0;
        object-fit: cover;
        visibility: hidden;
        transition: opacity 0.2s ease-in, visibility 0.2s;
      }
    `
  ]
})
export class AppPreviewComponent {
  @Input() links: string[] = [];

  loaded(event: Event) {
    (<HTMLElement>event.target).style.visibility = 'visible';
    (<HTMLElement>event.target).style.opacity = '1';
  }
}
