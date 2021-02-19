const fetch = require("node-fetch");

var bodyParser = require("body-parser");
var jsonParser = bodyParser.json();

const SMART_LATCH_ESP_API = "https://smart-latchxyz.xyz";

const smartLatchGet = (endpoint = "/healtcheck") => {
	return fetch(`${SMART_LATCH_ESP_API}${endpoint}`)
		.then((res) => res.json())
		.catch((err) => err);
};

const smartLatchPost = (endpoint = "/", data = {}) => {
	console.log("Heres is the data I am going to send out", data);
	return fetch(`${SMART_LATCH_ESP_API}${endpoint}`, {
		method: "POST", // *GET, POST, PUT, DELETE, etc.
		mode: "cors", // no-cors, *cors, same-origin
		cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
		credentials: "same-origin", // include, *same-origin, omit
		headers: {
			"Content-Type": "application/json",
			// 'Content-Type': 'application/x-www-form-urlencoded',
		},
		redirect: "follow", // manual, *follow, error
		referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
		body: JSON.stringify(data), // body data type must match "Content-Type" header
	})
		.then((res) => res.json())
		.catch((err) => err);
};

const openDoor = ({ doorId, userId }) => {
	return smartLatchPost("/openDoor", { doorId, userId });
};

exports.toggleLatch = (req, res) => {
	const desiredState = req.query && req.query.state;
	openDoor({ doorId: 69, userId: 420 })
		.then((data) => {
			console.log("response from google compute engine");
			console.log(data);
			res.status(200).send({ Authorization: "ok", newDoorState: desiredState });
		})
		.catch((err) => {
			console.log(err);
		});
};

exports.healthCheck = (req, res) => {
	res.send({ message: "smart latch server is running" });
};
