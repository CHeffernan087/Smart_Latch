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
	res.send({ Authorization: "ok", newDoorState: desiredState });
});

/*
web socket stuff
*/

const webSocketServer = new WebSocket.Server({
	server,
});

webSocketServer.on("connection", (webSocket) => {
	webSocket.on("message", (data) => {
		webSocketServer.clients.forEach((client) => {
			if (client !== webSocket && client.readyState === WebSocket.OPEN) {
				client.send(data);
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
