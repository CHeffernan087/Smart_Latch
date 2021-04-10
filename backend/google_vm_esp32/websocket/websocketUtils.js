"use strict";

exports.parseMessageFromBoard = (data) => {
	const keyValues = data.split(",");
	const resObj = keyValues.reduce((acc, el) => {
		const [key, ...value] = el.split(":");
		const parsedValue = value.reduce(
			(acc, el, index) => (el += `${index > 0 ? ":" : ""}${acc}`),
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
		default:
			return;
			break;
	}
};
