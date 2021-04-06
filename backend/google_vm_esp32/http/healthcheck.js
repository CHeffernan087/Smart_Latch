"use strict";

exports.healthchecks = (app) => {
	app.get("/healthcheck", (req, res) => {
		res.send({ message: "smart latch server is running" });
	});
	app.post("/postHealthcheck", (req, res) => {
		res.send({ message: "smart latch server is running" });
	});
};
