"use strict";
const WebSocket = require("ws");
const {
	parseMessageFromBoard,
	handleMessageFromBoard,
} = require("./websocketUtils");

exports.initialiseWebsocketServer = (
	webSocketServer,
	redisSubscriber,
	openConnections
) => {
	webSocketServer.on("connection", (webSocket) => {
		//todo. We need to add the users doorId in here
		console.log("new board connected");
		webSocket.send("boardIdReq");
		webSocket.on("message", (data) => {
			const messageObj = parseMessageFromBoard(data);
			handleMessageFromBoard(
				messageObj,
				webSocket,
				openConnections,
				redisSubscriber
			);
		});
		webSocket.on("close", () => {
			redisSubscriber.unsubscribe(webSocket.id);
			delete openConnections[webSocket.id];
		});
	});
};
