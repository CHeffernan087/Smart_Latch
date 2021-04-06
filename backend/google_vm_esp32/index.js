"use strict";
/* requirements */
var bodyParser = require("body-parser");
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const app = express();
const { initialiseWebsocketServer } = require("./websocket/websocketServer");
const { healthchecks } = require("./http/healthcheck");
const { home } = require("./http/home");
const { initialiseRedisClient, redisSubscriber } = require("./redis/redis");

const port = process.env.PORT || 8080;
const openConnections = {};

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

const webSocketServer = new WebSocket.Server({
	server,
});
initialiseWebsocketServer(webSocketServer, redisSubscriber, openConnections);
/*
PUB/SUB redis stuff
*/
initialiseRedisClient(redisSubscriber, openConnections);
/*
activate server
*/
server.listen(port, () => {
	console.log(`Server is now running on port ${port}`);
});
