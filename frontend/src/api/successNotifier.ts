import { ElMessage } from 'element-plus'

export type SuccessNotifier = (message: string) => void
let notifier: SuccessNotifier = (message) => ElMessage.success(message)

export function setSuccessNotifier(next: SuccessNotifier): void { notifier = next }
export function notifySuccess(message: string): void { notifier(message) }
