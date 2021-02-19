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

const sampleDoorId = 31415;
const sampleWebsocketConnection = 926535;

const openConnections = {
	sampleDoorId: sampleWebsocketConnection,
};

app.get("/healthcheck", (req, res) => {
	res.send({ message: "smart latch server is running" });
});

app.get("/", (req, res, next) => {
	next("Error: Not found. Please specify a valid endpoint");
});

app.post("/openDoor", (req, res) => {
	const { body } = req;
	const doorId = body && body.doorId;
	console.log("doorId : ", doorId);
	const userId = body && body.userId;

	if (doorId && openConnections[doorId]) {
		const client = openConnections[doorId];
		if (client.readyState === WebSocket.OPEN) {
			client.send("Open up ya bollix");
		}
		res.status(200).send({ message: "Door opening..." });
	}
	res.status(404).send({ error: "This door is not online" });
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