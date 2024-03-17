export class PostItem {
  constructor(
    public index: number,
    public title: string,
    public url: string,
    public hosts: string,
    public postId: number,
    public threadId: number
  ) {}
}
