import { authHandlers } from './auth'

// Mỗi FE dev thêm handler của feature mình (catalog, scheduling, learning, billing…) theo
// contracts/rest-api.md, rồi chuyển sang API thật khi unit tương ứng merge (D4).
export const handlers = [...authHandlers]
