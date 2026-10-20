import { authHandlers } from './auth'
import { notificationHandlers } from './notifications'
import { schedulingHandlers } from './scheduling'

export const handlers = [...authHandlers, ...notificationHandlers, ...schedulingHandlers]
