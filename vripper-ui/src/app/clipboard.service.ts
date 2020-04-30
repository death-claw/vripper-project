import {Observable, Subject} from 'rxjs';
import {ElectronService} from 'ngx-electron';
import {Injectable} from '@angular/core';
import {Clipboard} from 'electron';
import {HttpClient} from '@angular/common/http';
import {Settings} from './domain/settings.model';
import {ServerService} from './server-service';

@Injectable({
  providedIn: 'root'
})
export class ClipboardService {
  private links$: Subject<string> = new Subject();
  private interval: NodeJS.Timer;
  private lastText = '';

  constructor(
    private electronService: ElectronService,
    private serverService: ServerService,
    private httpClient: HttpClient
  ) {}

  init(settings?: Settings) {
    if (!settings) {
      this.httpClient.get<Settings>(this.serverService.baseUrl + '/settings').subscribe(
        data => {
          this._init(data);
        },
        error => {
          console.error(error);
        }
      );
      return;
    } else {
      this._init(settings);
    }
  }

  _init(settings: Settings) {
    if (!this.electronService.isElectronApp) {
      console.log('Clipboard deactive, not an electron app');
      return;
    }
    if (this.interval != null) {
      clearInterval(this.interval);
    }
    if (!settings.desktopClipboard) {
      return;
    }

    const clipboard: Clipboard = this.electronService.clipboard;
    this.interval = setInterval(() => {
      const text = clipboard.readText();

      if (this.textHasDiff(text, this.lastText)) {
        this.lastText = text;
        if (text.indexOf('https://vipergirls.to/threads') !== -1) {
          this.links$.next(text);
        }
      }
    }, 500);
  }

  private textHasDiff(a, b) {
    return a && b !== a;
  }

  get links(): Observable<string> {
    return this.links$.asObservable();
  }
}
