FROM node:22-bookworm-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY dist ./dist
COPY config ./config
ENV SKILL_MCP_CONFIG_DIR=/app/config
ENV SKILL_MCP_DATA_DIR=/tmp/skill-data
ENTRYPOINT ["node", "--experimental-sqlite", "dist/index.js", "security-watch"]
