/* requirements */
const express = require("express");
const port = process.env.PORT || 3000;

/*
server definition and config
*/

const server = express();

/*
end points
*/

server.get("/healthcheck", (req, res) => {
	res.send({ message: "smart latch server is running" });
});

server.get("/", (req, res, next) => {
	next("Error: Not found. Please specify a valid endpoint");
});

server.get("/toggleLatch", (req, res) => {
	const desiredState = req.query && req.query.state;
	res.send({ Authorization: "ok", newDoorState: desiredState });
});

/*
activate server
*/
server.listen(port, () => {
	console.log(`Server is now running on port ${port}`);
});
