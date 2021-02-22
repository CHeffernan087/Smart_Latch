const fetch = require("node-fetch");
const Firestore = require('@google-cloud/firestore');
const firestoreDb = new Firestore({
	projectId: 'smart-latch',
	keyFilename: 'smart-latch-db45150c5709.json',
});

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
		openDoor({ doorId: sampleDoorId, userId: 420 })
			.then((response) => {
				return new Promise((resolve, reject) => {
					if (response.status == 200) {
						resolve(response.json());
					} else reject(response.json());
				});
			})
			.then((data) => {
				res
					.status(200)
					.send({ Authorization: "ok", newDoorState: desiredState });
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

//curl -d "email=joeblogs@gmail.com&firstname=joe&lastname=blogs" -X POST http://localhost:8080/ 
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

	const docRef = firestoreDb.collection('users').doc(email);

	docRef.set({
		firstname: firstname,
		lastname: lastname,
		email: email
	}).then((data) => {
		res.status(200).send({ message: `Successfully added ${email}, ${firstname} ${lastname}`, data: data })
	}).catch((err) => {
		res.status(400).send({ error: err })
	});
};

//curl -d "email=joeblogs@gmail.com" -X DELETE http://localhost:8080/
exports.deleteUser = (req, res) => {
	if (req.method != "DELETE") {
		res.status(400).send({ error: "Needs to be a DELETE request" });
	}
	if (req.body.hasOwnProperty("email") === false)
	{
		res.status(400).send({ error : "Need to specify email in DELETE"});
	}
	email = req.body.email;
	firestoreDb.collection('users').doc(email).delete().then((data) => {
		res.status(200).send({message: `Successfully deleted ${email} from DB`, data: data})
	}).catch((err) => {
		res.status(400).send({error: err})
	});
}
