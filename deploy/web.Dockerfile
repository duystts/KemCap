# Frontend build + Caddy phục vụ cùng domain với API (R13: cookie SameSite=Strict cần cùng site)
FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# API cùng origin → đường dẫn tương đối, không phụ thuộc domain
ENV VITE_API_URL=/api/v1 VITE_USE_MOCKS=false
RUN npm run build

FROM caddy:2
COPY --from=build /src/dist /srv
