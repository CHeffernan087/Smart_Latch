const fetch = require("node-fetch");

const cloud_url = "https://europe-west2-smart-latch.cloudfunctions.net";

const cloud_get = (endpoint = "/setLockState?doorId=31415&isLocked=true") => {
	return fetch(`${cloud_url}${endpoint}`)
		.then((res) => res.json())
		.catch((err) => err);
};

const setDoorLocked = (doorId, isLocked) => {
	return cloud_get(`/setLockState?doorId=${doorId}&isLocked=${isLocked}`);
};
