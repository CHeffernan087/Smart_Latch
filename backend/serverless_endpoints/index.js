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