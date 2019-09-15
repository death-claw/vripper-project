import { HttpClient } from '@angular/common/http';
import { Injectable, Renderer2 } from '@angular/core';
import { ServerService } from './server-service';
import { tap } from 'rxjs/operators';

@Injectable()
export class AppService {

  constructor(private httpClient: HttpClient, private serverService: ServerService) {}

  darkTheme = false;
  _renderer: Renderer2;
  set renderer(renderer: Renderer2) {
    this._renderer = renderer;
  }

  updateTheme(darkTheme: boolean) {
    this.darkTheme = darkTheme;
    if (this.darkTheme) {
      this._renderer.addClass(document.body, 'dark-theme');
      this._renderer.removeClass(document.body, 'light-theme');
    } else {
      this._renderer.addClass(document.body, 'light-theme');
      this._renderer.removeClass(document.body, 'dark-theme');
    }
    this.httpClient.post<Theme>(this.serverService.baseUrl + '/settings/theme', {
      darkTheme: this.darkTheme
    }).subscribe();
  }

  loadTheme() {
    return this.httpClient
    .get<Theme>(this.serverService.baseUrl + '/settings/theme')
    .pipe(
      tap(theme => this.updateTheme(theme.darkTheme))
    );
  }
}

export interface Theme {
  darkTheme: boolean;
}