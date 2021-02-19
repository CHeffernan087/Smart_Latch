/* requirements */
const WebSocket = require("ws");
const http = require("http");
const express = require("express");
const port = process.env.PORT || 3000;
const {OAuth2Client} = require('google-auth-library');
// todo: move this to json file
const APP_GOOGLE_CLIENT_ID = "203181786221-4rllfugkn3o5ulgdn9gtpags73tbek1g.apps.googleusercontent.com";

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

app.post("/verifyUser", (req, res, next) => {
	const client = new OAuth2Client(APP_GOOGLE_CLIENT_ID);
	let payload; // lets just send the payload back for now to see what it is 
	
	// ugly for the moment, just trying to see how the java POST is built...
	let token = req.query.idToken;
	console.log(`Query: ${JSON.stringify(req.query)}`);
	console.log(`Params: ${JSON.stringify(req.params)}`);
	console.log(`Body: ${JSON.stringify(req.body)}`);

	
	
	async function verify() {
	const ticket = await client.verifyIdToken({
		idToken: token,
		audience: APP_GOOGLE_CLIENT_ID, 
	});
	payload = ticket.getPayload();
	console.log(payload);
	const userid = payload['sub'];
	// use userid as a unique identifer for the user in our database 

	}
	verify().catch(console.error);

	res.send({success: payload});
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
