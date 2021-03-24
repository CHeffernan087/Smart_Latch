const { isRequestAllowed, smartLatchPost } = require("./utils");
const {
	addDoorToUser,
	getUserDoors,
	isDoorActive,
	isAuthorised,
	setDoorAdmin,
} = require("./databaseApi");

exports.getUserDoors = (req, res) => {
	const email = req.query && req.query.email;

	getUserDoors(email).then((doors) => {
		res.send({ message: "Not implemented yet.", doors });
	});
};

const openDoor = ({ doorId, email }) => {
	return smartLatchPost("/openDoor", { doorId, userId: email });
};

exports.registerDoor = (req, res) => {
	if (!isRequestAllowed(req, "POST")) {
		return res.status(401).send({
			error: "No such endpoint. Did you specify the wrong request type?",
		});
	}
	const { doorId, email } = req.body;

	if (doorId && email) {
		isDoorActive(doorId)
			.then((doorActive) => {
				if (doorActive) {
					return setDoorAdmin(email, doorId);
				} else {
					throw "Door is not currently active";
				}
			})
			.then(() => addDoorToUser(email, doorId))
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
	const { email } = req.user;
	// todo : add some validation here if the user is allowed to open the door

	return isAuthorised(email, doorId)
		.then((userIsAuthorized) => {
			if (userIsAuthorized) {
				return openDoor({ doorId, email });
			} else {
				throw new Error("User not Authorised");
			}
		})
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
				};
			}
		})
		.then(({ status, response }) => {
			return res.status(status).send({ response: response });
		})
		.catch((error) => res.status(400).send({ error }));

	// todo close connection on the board
	// ;
};
