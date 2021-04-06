"use strict";
const redis = require("redis");
const WebSocket = require("ws");

const REDISHOST = process.env.REDISHOST || "redis.smart-latchxyz.xyz";
const REDISPORT = process.env.REDISPORT || 6379;

var subscriber = redis.createClient(REDISPORT, REDISHOST);
subscriber.on("error", (err) => console.error("ERR:REDIS:", err));

exports.initialiseRedisClient = (redisSubscriber, openConnections) => {
	redisSubscriber.on("message", function (doorId, payload) {
		const message = JSON.parse(payload);
		const { userId } = message;
		console.log(`[${userId}]: wants to interact with door ${doorId}`);
		if (doorId && openConnections[doorId] != undefined) {
			const client = openConnections[doorId];
			if (client.readyState === WebSocket.OPEN) {
				client.send("ToggleLatch");
			}
		}
	});
};

exports.redisSubscriber = subscriber;
