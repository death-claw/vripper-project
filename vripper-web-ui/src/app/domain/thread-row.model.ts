import { Thread } from './thread.model';

export class ThreadRow extends Thread {
  constructor(link: string, title: string, threadId: number, total: number) {
    super(link, title, threadId, total);
  }
}
