const fetch = require("node-fetch");

const cloud_url = "https://europe-west2-smart-latch.cloudfunctions.net";

const cloud_get = (endpoint = "/setLockState?doorId=31415&isLocked=true") => {
	return fetch(`${cloud_url}${endpoint}`)
		.then((res) => res.json())
		.catch((err) => err);
};

exports.toggleLockState = (doorId) => {
	return cloud_get(`/toggleLockState?doorId=${doorId}`);
};

exports.setLockState = (doorId, isLocked) => {
	return cloud_get(`/setLockState?doorId=${doorId}&isLocked=${isLocked}`);
};
