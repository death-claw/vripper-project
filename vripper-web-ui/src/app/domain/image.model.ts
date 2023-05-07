export class Image {
  constructor(
    public postId: string,
    public url: string,
    public status: string,
    public index: number,
    public current: number,
    public total: number
  ) {}
}
