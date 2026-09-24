export class ApiError extends Error {
  constructor(
    message: string,
    public readonly httpStatus?: number,
    public readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
