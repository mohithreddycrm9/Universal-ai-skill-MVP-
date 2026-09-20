FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --omit=dev=false
COPY tsconfig.json tsconfig.build.json ./
COPY src ./src
RUN npm run build

FROM node:22-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/package.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY config ./config
COPY examples ./examples
RUN useradd --create-home --uid 10001 skillmcp \
  && mkdir -p /app/data \
  && chown -R skillmcp:skillmcp /app/data
USER 10001
EXPOSE 43177
ENTRYPOINT ["node", "--experimental-sqlite", "dist/cli.js"]
CMD ["serve", "--http", "--host", "0.0.0.0", "--port", "43177"]
