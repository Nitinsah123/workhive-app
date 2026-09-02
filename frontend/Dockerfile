# =============================================================================
# WorkHive Frontend - Production Dockerfile for Cloud Platforms (Railway, Render)
# =============================================================================

# Stage 1: Build production bundle
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency manifests
COPY package.json pnpm-lock.yaml* package-lock.json* ./

# Install dependencies
RUN if [ -f pnpm-lock.yaml ]; then npm install -g pnpm && pnpm install --frozen-lockfile; else npm install; fi

# Copy source files
COPY . .

# Build argument for backend API URL during build
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

# Stage 2: Serve using high-performance Nginx Alpine
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Clean default web root
RUN rm -rf ./*

# Copy built distribution assets
COPY --from=builder /app/dist ./

# Copy template configuration (Nginx automatically performs envsubst on /etc/nginx/templates)
COPY nginx.conf /etc/nginx/templates/default.conf.template

ENV PORT=80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
