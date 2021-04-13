"use strict";
// const http = require("http");
const redis = require("redis");
const REDISHOST = process.env.REDISHOST || "redis.smart-latchxyz.xyz";
const REDISPORT = process.env.REDISPORT || 6379;

const publisher = redis.createClient(REDISPORT, REDISHOST);
publisher.on("error", (err) => console.error("ERR:REDIS:", err));

/**
 Used to publish updates to the redis server which all the active doors are subscribed to 
*/
exports.publishUpdate = (doorId, userId) => {
	return new Promise((resolve, reject) => {
		console.log("[CHEFF]: publishing the following topic: ", doorId);
		publisher.publish(doorId, `{"userId":\"${userId}\"}`, (error) => {
			if (error) {
				reject(error);
			}
			resolve();
		});
	});
};
