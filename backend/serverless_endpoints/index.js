const fetch = require("node-fetch");

const SMART_LATCH_ESP_API = "https://smart-latchxyz.xyz";
const sampleDoorId = "31415";

const { OAuth2Client } = require("google-auth-library");
const APP_GOOGLE_CLIENT_ID =
	"203181786221-3uljiupllmu130gv7o6nei0c0vsuvb70.apps.googleusercontent.com"; // TODO: move to JSON file?

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
	const token = req.query.idToken;
	let payload;

	async function verify() {
		const ticket = await client.verifyIdToken({
			idToken: token,
			audience: APP_GOOGLE_CLIENT_ID,
		});

		payload = ticket.getPayload();
		const userid = payload["sub"];
		// TODO: Check if userid is in DB, if not register them.
		// another key from 'payload' we might need is 'email', after that the rest isn't too important
	}

	try {
		verify();
		res.send({ success: true });
	} catch (e) {
		console.log(e);
		res.send({ success: false });
	}
};

exports.healthcheck = (req, res) => {
	res.send({ message: "smart latch server is running" });
};
