const fetch = require("node-fetch");
const admin = require('firebase-admin');
admin.initializeApp();

const firestoreDb = admin.firestore();
//TODO comment out
// const Firestore = require('@google-cloud/firestore');
// const firestoreDb = new Firestore({
// 	projectId: 'smart-latch',
// 	keyFilename: '../../../smart-latch-db45150c5709.json',
// });

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

exports.registerDoor = (req, res) => {
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

exports.getUserDoors = (req, res) => {
	const userId  = req.query && req.query.userId; 
	if (userId === "1234") { //testID
		res.send({ message: "Test ID", doors: ["Test Door ID"]})
	} else {
		// TODO: ping the door database/table with the userID, ask for this user's doors.
		// ---> send users door's back if they have any in an array ["door1", "door2"]
		res.send({message: "Not implemented yet.", doors: []});
	}
}

exports.healthcheck = (req, res) => {
	res.send({ message: "smart latch server is running" });
};

async function isDoorActive(doorId) {
	return (await firestoreDb.collection('Doors').doc(doorId).get()).get('IsActive');
}

async function isAuthorised(email, doorId) {
	userDoc = firestoreDb.collection('Users').doc(email);
	doors = firestoreDb.collection('Doors');
	const doorsAuthorisedForUser = (await doors.where('Authorised', 'array-contains', userDoc).get()).docs;
	for (let index = 0; index < doorsAuthorisedForUser.length; index++) {
		const doorDocument = doorsAuthorisedForUser[index];
		console.log(doorDocument.id);
		if (doorDocument.id == doorId) {
			return true;
		}
	}
	return false;
}

async function setDoorAdmin(email, doorId) {
	doorDocument = firestoreDb.collection('Doors').doc(doorId)
	userDoc = firestoreDb.collection('Users').doc(email);
	await setDoorAsActive(doorId);
	await addAsAuthorised(email, doorId);
	return doorDocument.update({ Admin: userDoc })
}

function setDoorAsActive(doorId) {
	doorDocument = firestoreDb.collection('Doors').doc(doorId)
	return doorDocument.update({ IsActive: true })
}

function addAsAuthorised(email, doorId) {
	doorDocument = firestoreDb.collection('Doors').doc(doorId)
	userDoc = firestoreDb.collection('Users').doc(email)
	return doorDocument.update({
		Authorised: admin.firestore.FieldValue.arrayUnion(userDoc)
	});
}

function registerAsUser(email, firstname, lastname) {
	const docRef = firestoreDb.collection('Users').doc(email);
	return docRef.set({
		firstname: firstname,
		lastname: lastname,
		email: email
	})
}

//curl -d "email=joeblogs@gmail.com&firstname=joe&lastname=blogs" -X POST http://localhost:8080/ 
//curl -d "email=joeblogs@gmail.com&firstname=joe&lastname=blogs" -X POST https://europe-west2-smart-latch.cloudfunctions.net/registerUser --ssl-no-revoke
exports.registerUser = (req, res) => {
	if (req.method != "POST") {
		res.status(400).send({ error: "Needs to be a POST request" });
	}
	const keys = ["email", "firstname", "lastname"];
	const hasAllKeys = keys.every(key => req.body.hasOwnProperty(key));
	if (hasAllKeys === false) {
		res.status(400).send({ error: "Missing values in POST request" });
	}
	email = req.body.email;
	firstname = req.body.firstname;
	lastname = req.body.lastname;
	const userIsAuthorized = false;
	if (userIsAuthorized) {
		registerAsUser(email, firstname, lastname).then((data) => {
			res.status(200).send({ message: `Successfully added ${email}, ${firstname} ${lastname}`, data: data })
		}).catch((err) => {
			res.status(400).send({ error: err })
		});
	}
};

function deleteUserFromDB(email) {
	return firestoreDb.collection('Users').doc(email).delete();
}


//curl -d "email=joeblogs@gmail.com" -X DELETE http://localhost:8080/
//curl -d "email=joeblogs@gmail.com" -X DELETE https://europe-west2-smart-latch.cloudfunctions.net/deleteUser --ssl-no-revoke
exports.deleteUser = (req, res) => {
	if (req.method != "DELETE") {
		res.status(400).send({ error: "Needs to be a DELETE request" });
	}
	if (req.body.hasOwnProperty("email") === false) {
		res.status(400).send({ error: "Need to specify email in DELETE" });
	}
	email = req.body.email;
	const userIsAuthorized = false;
	if (userIsAuthorized) {
		deleteUserFromDB(email).then((data) => {
			res.status(200).send({ message: `Successfully deleted ${email} from DB`, data: data })
		}).catch((err) => {
			res.status(400).send({ error: err })
		});
	}
}