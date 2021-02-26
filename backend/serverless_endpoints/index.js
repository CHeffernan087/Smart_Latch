const fetch = require("node-fetch");

const SMART_LATCH_ESP_API = "https://smart-latchxyz.xyz";
const sampleDoorId = "31415";

const { OAuth2Client } = require("google-auth-library");
const e = require("express");
const APP_GOOGLE_CLIENT_ID =
	"639400548732-9ga9sg95ao0drj5sdtd3v561adjqptbr.apps.googleusercontent.com";

const smartLatchGet = (endpoint = "/healtcheck") => {
	return fetch(`${SMART_LATCH_ESP_API}${endpoint}`)
		.then((res) => res.json())
		.catch((err) => err);
};

const getRequestType = (req) => req.method;

const isRequestAllowed = (req, intendedReqType) =>
	getRequestType(req) === intendedReqType;

const smartLatchPost = (endpoint = "/", data = {}) => {
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
	});
};

const openDoor = ({ doorId, userId }) => {
	return smartLatchPost("/openDoor", { doorId, userId });
};

exports.toggleLatch = (req, res) => {
	const desiredState = req.query && req.query.state;
	const doorId = req.query && req.query.doorId;
	const userId = req.query && req.query.userId;
	// todo : add some validation here if the user is allowed to open the door
	const userIsAuthorized = true;
	if (userIsAuthorized) {
		return openDoor({ doorId: sampleDoorId, userId: 420 })
			.then((response) => {
				console.log("Here is the status :", response.status);
				if (response.status === 200) {
					return {
						status: 200,
						response: { Authorization: "ok", newDoorState: desiredState },
					};
				} else {
					return {
						status: 400,
						response: "Door not online",
						// message: response.json(),
					};
				}
			})
			.then(({ status, response }) => {
				res.status(status).send({ response: response });
			})
			.catch((err) => {
				console.log(err);
				res.status(400).send({ error: err });
			});
	} else {
		// todo close connection on the board
		res.status(400).send({ error: "You are not authorised to open this door" });
	}
};

exports.verifyUser = async (req, res) => {
	const client = new OAuth2Client(APP_GOOGLE_CLIENT_ID);
	const token = req.query && req.query.idToken;

	let payload = null;

	if (!token) {
		res.send({ error: "No 'idToken' parameter provided." });
		return;
	}

	async function verify() {
		const ticket = await client.verifyIdToken({
			idToken: token,
			audience: APP_GOOGLE_CLIENT_ID,
		});

		payload = ticket.getPayload();
		const userid = payload["sub"]; // Todo: can use this as a unique id for the DB if necessary?
	}
	verify()
		.then(() => {
			res.send({ success: true });
		})
		.catch((e) => {
			console.log(e);
			res.send({ success: false, error: "Token failed verification." });
		});
};

exports.registerDoor2 = (req, res) => {
	if (!isRequestAllowed(req, "POST")) {
		return res.status(401).send({
			error: "No such endpoint. Did you specify the wrong request type?",
		});
	}
	const { doorId, userId } = req.body;
	if (doorId && userId) {
		return res.status(200).send({ message: "Success! New door added" });
	} else {
		const missingFields = [];
		if (!doorId) {
			missingFields.unshift("doorId");
		}
		if (!userId) {
			missingFields.unshift("userId");
		}
		return res
			.status(400)
			.send({ error: `Missing field(s) ${missingFields.toString()}` });
	}
};

exports.healthcheck = (req, res) => {
	res.send({ message: "smart latch server is running" });
};
