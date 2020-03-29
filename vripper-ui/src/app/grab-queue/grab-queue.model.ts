export class GrabQueueState {
  constructor(
    public type: string,
    public link: string,
    public threadId: string,
    public postId: string,
    public removed: boolean,
    public count: number,
    public loading: boolean
  ) {}
}
