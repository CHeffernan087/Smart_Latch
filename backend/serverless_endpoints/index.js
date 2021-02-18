const esp_api = s;
exports.myFunction = (req, res) => {
	const message = "Hello node!";
	res.status(200).send(message);
};

exports.toggleLatch = (req, res) => {
	const desiredState = req.query && req.query.state;
	// todo : send message to the VM
	// webSocketServer.clients.forEach((client) => {
	// 	client.send("[SERVER MESSAGE]: Open the door!");
	// });
	res.status(200).send({ Authorization: "ok", newDoorState: desiredState });
};

exports.healthCheck = (req, res) => {
	res.send({ message: "smart latch server is running" });
};
