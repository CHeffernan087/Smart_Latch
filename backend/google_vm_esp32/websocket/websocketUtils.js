"use strict";
const { setLockState } = require("../cloud functions api/cloudFunctions");

exports.parseMessageFromBoard = (data) => {
	const keyValues = data.split(",");
	const resObj = keyValues.reduce((acc, el) => {
		const [key, ...value] = el.split(":");
		const parsedValue = value.reduce(
			(acc, el, index) => (acc += `${index > 0 ? ":" : ""}${el}`),
			""
		);
		return {
			[key]: parsedValue,
			...acc,
		};
	}, {});
	return resObj;
};

exports.handleMessageFromBoard = (
	messageObj,
	client,
	openConnections,
	subscriber
) => {
	const { message } = messageObj;
	switch (message) {
		case "boardIdRes":
			const { doorId } = messageObj;
			openConnections[doorId] = client;
			client.id = doorId;
			subscriber.subscribe(doorId);
			console.log(`added ${doorId} to the list of open web socket connections`);
			break;
		case "latchUpdate":
			const { status } = messageObj;
			const clientId = client.id;
			const locked = status == "closed" ? true : false;
			console.log(`updating ${clientId} locked status to ${locked}`);
			setLockState(clientId, locked)
				.then(() => {
					client.send("[SUCCESS]: lock status update successful");
				})
				.catch(() => {
					client.send("[ERROR]: lock status update failed");
				});
		default:
			return;
			break;
	}
};
