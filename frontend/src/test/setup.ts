import '@testing-library/jest-dom/vitest'
import { configure } from '@testing-library/react'

// Mặc định 1 giây dễ hụt khi máy đang bận, làm test tìm phần tử lỗi ngẫu nhiên
configure({ asyncUtilTimeout: 5000 })
