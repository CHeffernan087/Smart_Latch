const { isRequestAllowed, smartLatchPost } = require("./utils");
const {
	addDoorToUser,
	getUserDoors,
	isDoorActive,
	setDoorAdmin,
	getDoorDetails,
	setDoorNfcId,
} = require("./databaseApi");

exports.getUserDoors = (req, res) => {
	const email = req.query && req.query.email;
	let doorDetails = {};
	let doorCount = 0; 
	getUserDoors(email).then((doors) => {
		doors.forEach((door) => {
			getDoorDetails(door)
				.then((details) => {
					doorDetails = {
						...doorDetails,
						[door]: details,
					}
					doorCount++;
					if (doors.length === doorCount) {
						sendDoorResponse(doors);
					}
				}).catch((e) => {
					res.send({error: e});
				});
		})
	})
	.catch((e) => {
		res.send({ error: e});
	});

	function sendDoorResponse(doors) {
			res.send({ doors, doorDetails });
	}
};

const openDoor = ({ doorId, userId }) => {
	return smartLatchPost("/openDoor", { doorId, userId });
};

exports.registerDoor = (req, res) => {
	if (!isRequestAllowed(req, "POST")) {
		return res.status(401).send({
			error: "No such endpoint. Did you specify the wrong request type?",
		});
	}
	const { doorId, email, nfcId } = req.body;

	if (doorId && email) {
		isDoorActive(doorId)
			.then((doorActive) => {
				if (doorActive) {
					return setDoorAdmin(email, doorId);
				} else {
					throw "Door is not currently active";
				}
			})
			// .then(() => addDoorToUser(email, doorId)) --> Door should already be assigned to the person making this call 
			.then(() => setDoorNfcId(doorId, nfcId))
			.then(() => res.status(200).send({ message: "Success! New door added" }))
			.catch((err) => res.status(401).send({ err }));
	} else {
		const missingFields = [];
		if (!doorId) {
			missingFields.unshift("doorId");
		}
		if (!email) {
			missingFields.unshift("email");
		}
		return res
			.status(400)
			.send({ error: `Missing field(s) ${missingFields.toString()}` });
	}
};

exports.toggleLatch = (req, res) => {
	const desiredState = req.query && req.query.state;
	const doorId = req.query && req.query.doorId;
	const userId = req.query && req.query.userId;
	// todo : add some validation here if the user is allowed to open the door
	const userIsAuthorized = true;
	if (userIsAuthorized) {
		return openDoor({ doorId: 31415, userId: 420 })
			.then((response) => {
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
				res.status(400).send({ error: err });
			});
	} else {
		// todo close connection on the board
		res.status(400).send({ error: "You are not authorised to open this door" });
	}
};
