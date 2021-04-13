const { isRequestAllowed, smartLatchPost } = require("./utils");
const {
	addDoorToUser,
	getUserDoors,
	isDoorActive,
	isAuthorised,
	setDoorAdmin,
	getDoorDetails,
	setDoorNfcId,
	setLockState,
	toggleLockState,
	updateDoorNfcState,
	getDoorLockState,
} = require("./databaseApi");
const { publishUpdate } = require("./redis");

exports.getUserDoors = (req, res) => {
	const email = req.query && req.query.email;
	let doorDetails = {};
	let doorCount = 0;
	getUserDoors(email)
		.then((doors) => {
			doors.forEach((door) => {
				getDoorDetails(door)
					.then((details) => {
						doorDetails = {
							...doorDetails,
							[door]: details,
						};
						doorCount++;
						if (doors.length === doorCount) {
							return sendDoorResponse(doors);
						}
					})
					.catch((e) => {
						return res.status(400).send({ error: e });
					});
			});
		})
		.catch((e) => {
			res.status(400).send({ error: e });
		});

	function sendDoorResponse(doors) {
		return res.send({ doors, doorDetails });
	}
};

const openDoor = ({ doorId, userId }) => {
	return publishUpdate(doorId, userId);
};

exports.registerDoor = (req, res) => {
	if (!isRequestAllowed(req, "POST")) {
		return res.status(404).send({
			// 404 --> Not found
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
	const { email } = req.user;
	// todo : add some validation here if the user is allowed to open the door
	return isAuthorised(email, doorId)
		.then((userIsAuthorized) => {
			if (userIsAuthorized) {
				return openDoor({ doorId, userId: email });
			} else {
				throw new Error("User not Authorised");
			}
		})
		.then(() => {
			return res
				.status(200)
				.send({ response: "Successfully published update" });
		})
		.catch((error) => {
			return res.status(400).send({ error });
		});

	// todo close connection on the board
	// ;
};

exports.nfcUpdate = (req, res) => {
	const { doorId } = req.query;
	if (doorId) {
		getDoorDetails(doorId)
			.then((doorObject) => {
				if (doorObject.ID === doorId) {
					updateDoorNfcState(doorId)
						.then(() => {
							res
								.send({
									message: "Successfully updated NFC state to true for 2FA.",
								})
								.status(200);
						})
						.catch((e) => res.send({ error: e }));
				} else {
					res.send({
						message: `You are using the wrong door in the app (${doorId}) for this NFC tag. `,
					});
				}
			})
			.catch((e) => {
				res.send({ err: e }).status(500);
			});
	} else {
		const missingFields = [];
		if (!doorId) {
			missingFields.unshift("doorId");
		}
		return res
			.status(400)
			.send({ error: `Missing field(s) ${missingFields.toString()}` });
	}
};

exports.setLockState = (req, res) => {
	let { doorId, isLocked } = req.query;
	isLocked = isLocked == "true";
	setLockState(doorId, isLocked)
		.then(() => {
			return res.status(200).send({ res: "200" });
		})
		.catch((err) => {
			return res.status(400).send(err);
		});
};

exports.toggleLockState = (req, res) => {
	let { doorId } = req.query;
	toggleLockState(doorId)
		.then(() => {
			return res.status(200).send({ res: "200" });
		})
		.catch((err) => {
			return res.status(400).send(err);
		});
};

exports.getLockState = (req, res) => {
	const { doorId } = req.query;
	getDoorDetails(doorId)
		.then((doorData) => {
			const state = doorData.locked;
			return res
				.send({
					locked: state,
				})
				.status(200);
		})
		.catch((err) => {
			return res.send({ error: err }).status(500);
		});
};
