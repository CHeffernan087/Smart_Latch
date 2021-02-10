const express = require("express");
const port = 8080;

const server = express();

server.get("/healthcheck", (req, res) => {
	res.send({ message: "server running..." });
});

server.listen(port, () => {
	console.log(`Server listening on port ${port}`);
});
