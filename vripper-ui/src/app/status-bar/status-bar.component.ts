import {SelectionService} from '../selection-service';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {DownloadSpeed} from '../domain/download-speed.model';
import {WsConnectionService} from '../ws-connection.service';
import {BehaviorSubject, Subject, Subscription} from 'rxjs';
import {GlobalState} from '../domain/global-state.model';

@Component({
  selector: 'app-status-bar',
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBarComponent implements OnInit, OnDestroy {
  constructor(private ws: WsConnectionService, private ngZone: NgZone, private selectionService: SelectionService) {
  }

  downloadSpeed$: Subject<DownloadSpeed> = new BehaviorSubject(new DownloadSpeed('0 B'));
  globalState$: Subject<GlobalState> = new BehaviorSubject(new GlobalState(0, 0, 0, 0));
  selected$: Subject<number> = new BehaviorSubject(0);
  subscriptions: Subscription[] = [];

  ngOnInit() {
    this.subscriptions.push(this.ws.speed$.subscribe(e => this.ngZone.run(() => this.downloadSpeed$.next(e))));
    this.subscriptions.push(this.ws.globalState$.subscribe(e => this.ngZone.run(() => this.globalState$.next(e))));
    this.subscriptions.push(this.selectionService.selected$.subscribe(selected => this.ngZone.run(() => this.selected$.next(selected.length))));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
