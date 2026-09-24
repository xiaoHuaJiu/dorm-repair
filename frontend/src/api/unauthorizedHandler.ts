export type UnauthorizedHandler = () => void | Promise<void>
let handler: UnauthorizedHandler = () => undefined

export function setUnauthorizedHandler(next: UnauthorizedHandler): void { handler = next }
export async function handleUnauthorized(): Promise<void> { await handler() }
