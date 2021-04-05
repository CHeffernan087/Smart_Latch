const { isRequestAllowed, smartLatchPost } = require("./utils");
const {
	addDoorToUser,
	getUserDoors,
	isDoorActive,
	setDoorAdmin,
	getDoorDetails,
	setDoorNfcId,
	updateDoorNfcState,
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
		console.log("[CHEFF debug]: HERE ARE THE DOOR DETAILS");
		console.log(doors);
		return res.send({ doors, doorDetails });
	}
};

const openDoor = ({ doorId, userId }) => {
	return publishUpdate(doorId, userId).then(() => {});
	// return smartLatchPost("/openDoor", { doorId, userId });
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
	const userId = req.query && req.query.userId;

	const userIsAuthorized = true;
	if (userIsAuthorized) {
		return openDoor({ doorId: 31415, userId: 420 })
			.then(() => {
				return res
					.status(200)
					.send({ response: "Successfully published update" });
			})

			.catch((err) => {
				res.status(400).send({ error: err });
			});
	} else {
		// todo close connection on the board
		res.status(400).send({ error: "You are not authorised to open this door" });
	}
};

exports.nfcUpdate = (req, res) => {
	const { nfcId, doorId } = req.body;

	if (nfcId && doorId) {
		getDoorDetails(doorId, nfcId)
			.then((doorObject) => {
				if (doorObject.nfcId === nfcId) {
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
		if (!email) {
			missingFields.unshift("email");
		}
		return res
			.status(400)
			.send({ error: `Missing field(s) ${missingFields.toString()}` });
	}
};
