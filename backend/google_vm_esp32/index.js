/* requirements */
var bodyParser = require("body-parser");
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const {OAuth2Client} = require('google-auth-library');

const port = process.env.PORT || 3000;
const APP_GOOGLE_CLIENT_ID = "203181786221-3uljiupllmu130gv7o6nei0c0vsuvb70.apps.googleusercontent.com"; // TODO: move to JSON file? 

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
	const userId = body && body.userId;
	if (doorId && openConnections[doorId] != undefined) {
		const client = openConnections[doorId];
		if (client.readyState === WebSocket.OPEN) {
			client.send("ToggleLatch");
		}
		res.status(200).send({ message: "Door opening..." });
		return;
	}
	res.status(404).send({ error: "This door is not online" });
});

app.get("/verifyUser", async (req, res, next) => {
	const client = new OAuth2Client(APP_GOOGLE_CLIENT_ID);
	const token = req.query.idToken;
	let payload; 

	async function verify() {
		const ticket = await client.verifyIdToken({
			idToken: token,
			audience: APP_GOOGLE_CLIENT_ID, 
		});		

		payload = ticket.getPayload();
		const userid = payload['sub'];
		// TODO: Check if userid is in DB, if not register them. 
		// another key from 'payload' we might need is 'email', after that the rest isn't too important
	}

	try {
		verify()
		res.send({success: true});
	} catch (e) {
		console.log(e);
		res.send({success: false})
	}
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
		case "boardIdRes":
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
