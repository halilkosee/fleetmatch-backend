#!/usr/bin/env node

const baseUrl = process.env.BASE_URL || "http://localhost:8080";
const token = process.env.TOKEN;
const conversationId = process.env.CONVERSATION_ID;
const body = process.env.MESSAGE || `WebSocket smoke message ${Date.now()}`;

if (!token || !conversationId) {
  console.error("TOKEN and CONVERSATION_ID are required.");
  console.error("Example:");
  console.error("  TOKEN=<jwt> CONVERSATION_ID=<uuid> BASE_URL=http://localhost:8080 node scripts/e2e/websocket_smoke.js");
  process.exit(1);
}

if (typeof WebSocket === "undefined") {
  console.error("This script requires a Node.js runtime with global WebSocket support.");
  console.error("Use Node 22+ or test the same STOMP frames from a WebSocket client.");
  process.exit(1);
}

const wsUrl = baseUrl.replace(/^http:/, "ws:").replace(/^https:/, "wss:") + "/ws";
const destination = `/topic/conversations/${conversationId}`;
const sendDestination = `/app/conversations/${conversationId}/messages`;

let connected = false;
let subscribed = false;
let receivedMessage = false;

function frame(command, headers = {}, payload = "") {
  const headerLines = Object.entries(headers)
    .map(([key, value]) => `${key}:${value}`)
    .join("\n");

  return `${command}\n${headerLines}\n\n${payload}\0`;
}

function send(socket, command, headers, payload) {
  socket.send(frame(command, headers, payload));
}

function fail(message) {
  console.error(`WEBSOCKET E2E FAILED: ${message}`);
  process.exit(1);
}

const timeout = setTimeout(() => {
  fail("Timed out waiting for message event");
}, 15000);

const socket = new WebSocket(wsUrl, ["v12.stomp"]);

socket.addEventListener("open", () => {
  send(socket, "CONNECT", {
    "accept-version": "1.2",
    host: "localhost",
    Authorization: `Bearer ${token}`,
  });
});

socket.addEventListener("message", (event) => {
  const data = String(event.data);

  if (data.startsWith("CONNECTED")) {
    connected = true;
    send(socket, "SUBSCRIBE", {
      id: "conversation-smoke-subscription",
      destination,
      ack: "auto",
    });
    subscribed = true;

    send(socket, "SEND", {
      destination: sendDestination,
      "content-type": "application/json",
    }, JSON.stringify({ body }));
    return;
  }

  if (data.startsWith("ERROR")) {
    fail(data.replace(/\0/g, ""));
  }

  if (data.startsWith("MESSAGE") && data.includes(body)) {
    receivedMessage = true;
    clearTimeout(timeout);
    send(socket, "DISCONNECT", { receipt: "close" });
    socket.close();
    console.log("WEBSOCKET E2E PASSED");
    console.log(`Conversation ID: ${conversationId}`);
    console.log(`Message: ${body}`);
  }
});

socket.addEventListener("error", () => {
  fail("WebSocket connection error");
});

socket.addEventListener("close", () => {
  if (!connected) {
    fail("Connection closed before STOMP CONNECTED frame");
  }

  if (!subscribed) {
    fail("Connection closed before subscription");
  }

  if (!receivedMessage) {
    fail("Connection closed before receiving published message");
  }
});
