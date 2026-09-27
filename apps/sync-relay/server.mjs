#!/usr/bin/env node
/**
 * Minimal WebSocket relay for Universal AI mobile ↔ desktop sync.
 * Clients send encrypted SyncEnvelope JSON; relay broadcasts to other peers.
 *
 *   npm install && npm start
 *   ws://HOST:8787/sync
 */
import { createServer } from "node:http";
import { WebSocketServer } from "ws";

const PORT = Number(process.env.PORT || 8787);
const clients = new Set();

const server = createServer((_req, res) => {
  res.writeHead(200, { "Content-Type": "text/plain" });
  res.end("Universal AI sync relay. Connect via WebSocket /sync\n");
});

const wss = new WebSocketServer({ server, path: "/sync" });

wss.on("connection", (socket, req) => {
  const deviceId = req.headers["x-device-id"] || "unknown";
  clients.add(socket);
  socket.on("message", (data) => {
    for (const peer of clients) {
      if (peer !== socket && peer.readyState === peer.OPEN) {
        peer.send(data);
      }
    }
  });
  socket.on("close", () => clients.delete(socket));
  socket.send(JSON.stringify({ type: "welcome", deviceId, peers: clients.size }));
});

server.listen(PORT, () => {
  console.log(`Sync relay listening on ws://0.0.0.0:${PORT}/sync`);
});
