"use strict";
/* requirements */
var bodyParser = require("body-parser");
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const redis = require("redis");
const app = express();
const { initialiseWebsocketServer } = require("./websocket/websocketServer");
const { healthchecks } = require("./http/healthcheck");
const { home } = require("./http/home");

const port = process.env.PORT || 8080;
const REDISHOST = process.env.REDISHOST || "redis.smart-latchxyz.xyz";
const REDISPORT = process.env.REDISPORT || 6379;

var subscriber = redis.createClient(REDISPORT, REDISHOST);
subscriber.on("error", (err) => console.error("ERR:REDIS:", err));

/*
server definition and config
*/
app.use(bodyParser.json());
const server = http.createServer(app);
/*
end points
*/
healthchecks(app);
home(app);

/*
web socket stuff
*/

const openConnections = {};

const webSocketServer = new WebSocket.Server({
	server,
});
initialiseWebsocketServer(webSocketServer, subscriber, openConnections);
/*
PUB/SUB redis stuff
*/

subscriber.on("message", function (doorId, payload) {
	const message = JSON.parse(payload);
	const { userId } = message;
	console.log(`[${userId}]: wants to interact with door ${doorId}`);
	if (doorId && openConnections[doorId] != undefined) {
		const client = openConnections[doorId];
		if (client.readyState === WebSocket.OPEN) {
			client.send("ToggleLatch");
		}
	}
});

/*
activate server
*/
server.listen(port, () => {
	console.log(`Server is now running on port ${port}`);
});
