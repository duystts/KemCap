import { authHandlers } from './auth'
import { notificationHandlers } from './notifications'

export const handlers = [...authHandlers, ...notificationHandlers]
