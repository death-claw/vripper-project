export class PostItem {
  constructor(
    public index: number,
    public title: string,
    public url: string,
    public hosts: [{ first: string; second: number }],
    public postId: string,
    public threadId: string
  ) {}
}
