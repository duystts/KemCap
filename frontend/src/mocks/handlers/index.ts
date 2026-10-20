import { authHandlers } from './auth'
import { notificationHandlers } from './notifications'
import { schedulingHandlers } from './scheduling'
import { usersHandlers } from './users'
import { catalogHandlers } from './catalog'

export const handlers = [
  ...authHandlers,
  ...notificationHandlers,
  ...schedulingHandlers,
  ...usersHandlers,
  ...catalogHandlers,
]
