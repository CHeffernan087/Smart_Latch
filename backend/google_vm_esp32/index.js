/* requirements */
var bodyParser = require("body-parser");
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const port = process.env.PORT || 3000;

/*
server definition and config
*/
const app = express();
app.use(bodyParser.json());
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

app.post("/openDoor", (req, res) => {
	const { body } = req;
	const doorId = body && body.doorId;
	console.log("doorId : ", doorId);
	const userId = body && body.userId;
	console.log(openConnections[doorId]);
	if (doorId && openConnections[doorId]) {
		const client = openConnections[doorId];
		if (client.readyState === WebSocket.OPEN) {
			client.send("ToggleLatch");
		}
		res.status(200).send({ message: "Door opening..." });
		return;
	}
	res.status(404).send({ error: "This door is not online" });
});

/*
web socket stuff
*/

const openConnections = {};

const parseMessageFromBoard = (data) => {
	const keyValues = data.split(",");
	const resObj = keyValues.reduce((acc, el) => {
		const [key, value] = el.split(":");
		return {
			[key]: value,
			...acc,
		};
	}, {});
	return resObj;
};

const handleMessageFromBoard = (messageObj, client) => {
	const { message } = messageObj;
	switch (message) {
		case "greeting":
			const { doorId } = messageObj;
			openConnections[doorId] = client;
			client.id = doorId;
			console.log(`added ${doorId} to the list of open web socket connections`);
			break;
		default:
			return;
			break;
	}
};

const webSocketServer = new WebSocket.Server({
	server,
});

webSocketServer.on("connection", (webSocket) => {
	//todo. We need to add the users doorId in here
	console.log("new board connected");
	webSocket.send("boardIdReq");
	webSocket.on("message", (data) => {
		const messageObj = parseMessageFromBoard(data);
		handleMessageFromBoard(messageObj, webSocket);
	});
	webSocket.on("close", () => {
		delete openConnections[webSocket.id];
	});
});

/*
activate server
*/
server.listen(port, () => {
	console.log(`Server is now running on port ${port}`);
});
