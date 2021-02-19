/* requirements */
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
