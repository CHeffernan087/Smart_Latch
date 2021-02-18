/* requirements */
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const port = process.env.PORT || 3000;

/*
server definition and config
*/
const app = express();
const server = http.createServer(app);
/*
end points
*/

app.get("/healthcheck", (req, res) => {
	res.send({ message: "smart latch server is running" });
});

app.get("/", (req, res, next) => {
	next("Error: Not found. Please specify a valid endpoint");
});

app.get("/toggleLatch", (req, res) => {
	const desiredState = req.query && req.query.state;
	webSocketServer.clients.forEach((client) => {
		client.send("[SERVER MESSAGE]: Open the door!");
	});
	res.send({ Authorization: "ok", newDoorState: desiredState });
});

/*
web socket stuff
*/

const webSocketServer = new WebSocket.Server({
	server,
});

webSocketServer.on("connection", (webSocket) => {
	console.log("board trying to connect...");
	webSocket.on("message", (data) => {
		webSocketServer.clients.forEach((client) => {
			if (client === webSocket && client.readyState === WebSocket.OPEN) {
				client.send("[SERVER MESSAGE]: You are connected to the server :)");
			}
		});
	});
});

/*
activate server
*/
server.listen(port, () => {
	console.log(`Server is now running on port ${port}`);
});
