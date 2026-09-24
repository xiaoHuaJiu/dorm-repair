import { ElMessage } from 'element-plus'

export type ErrorNotifier = (message: string) => void
let notifier: ErrorNotifier = (message) => ElMessage.error(message)

export function setErrorNotifier(next: ErrorNotifier): void { notifier = next }
export function notifyError(message: string): void { notifier(message) }
