import {Settings} from './common/settings.model';
import {environment} from 'src/environments/environment';
import {MatDialog} from '@angular/material/dialog';
import {HttpClient} from '@angular/common/http';
import {Injectable, Renderer2} from '@angular/core';
import {ServerService} from './server-service';
import {tap} from 'rxjs/operators';
import {ScanComponent} from './scan/scan.component';
import {Observable} from 'rxjs';
import {BreakpointObserver, Breakpoints, BreakpointState} from '@angular/cdk/layout';
import {Title} from '@angular/platform-browser';

@Injectable({
  providedIn: 'root'
})
export class AppService {
  constructor(
    private httpClient: HttpClient,
    private serverService: ServerService,
    public dialog: MatDialog,
    private breakpointObserver: BreakpointObserver,
    private titleService: Title
  ) {
    this.titleService.setTitle('VRipper ' + environment.version);
  }

  private _darkTheme = false;
  private _settings: Settings;
  private _renderer: Renderer2;

  get settings(): Settings {
    return { ...this._settings };
  }

  get darkTheme(): boolean {
    return this._darkTheme;
  }

  set renderer(renderer: Renderer2) {
    this._renderer = renderer;
  }

  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

  updateTheme(darkTheme: boolean) {
    this._darkTheme = darkTheme;
    if (this._darkTheme) {
      this._renderer.addClass(document.body, 'dark-theme');
      this._renderer.removeClass(document.body, 'light-theme');
    } else {
      this._renderer.addClass(document.body, 'light-theme');
      this._renderer.removeClass(document.body, 'dark-theme');
    }
    this.httpClient
      .post<Theme>(this.serverService.baseUrl + '/settings/theme', {
        darkTheme: this._darkTheme
      })
      .subscribe();
  }

  updateSettings(settings: Settings) {
    this._settings = settings;
  }

  loadTheme() {
    return this.httpClient
      .get<Theme>(this.serverService.baseUrl + '/settings/theme')
      .pipe(tap(theme => this.updateTheme(theme.darkTheme)));
  }

  loadSettings() {
    return this.httpClient
      .get<Settings>(this.serverService.baseUrl + '/settings')
      .pipe(tap(settings => this.updateSettings(settings)));
  }

  scan(url?: string) {

    const scanDialog = this.dialog.open(ScanComponent, {
      width: '70%',
      height: '70%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: {url: url}
    });

    const smallDialogSubscription = this.isExtraSmall.subscribe(result => {
      if (result.matches) {
        scanDialog.updateSize('100%', '100%');
      } else {
        scanDialog.updateSize('70%', '70%');
      }
    });

    scanDialog.afterClosed().subscribe(() => {
      smallDialogSubscription.unsubscribe();
    });
  }
}

export interface Theme {
  darkTheme: boolean;
}
