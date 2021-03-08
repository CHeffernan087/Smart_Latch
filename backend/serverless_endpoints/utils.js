const { SecretManagerServiceClient } = require("@google-cloud/secret-manager");
const client = new SecretManagerServiceClient();
const fetch = require("node-fetch");
const SMART_LATCH_ESP_API = "https://smart-latchxyz.xyz";

const getRequestType = (req) => req.method;

exports.getUser = (req) => {
	const { email, firstName, lastName, id } = req.user;
	return { email, firstName, lastName, id };
};

exports.isRequestAllowed = (req, intendedReqType) =>
	getRequestType(req) === intendedReqType;

exports.readInJwtSecret = async () => {
	return readInSecret("SMART_LATCH_SECRET");
};

const readInSecret = async (secretName) => {
	try {
		const [data] = await client.accessSecretVersion({
			name: `projects/639400548732/secrets/${secretName}/versions/latest`,
		});
		const jwt_secret = data.payload.data.toString();
		return jwt_secret;
	} catch (e) {
		return "default";
	}
};

exports.smartLatchGet = (endpoint = "/healtcheck") => {
	return fetch(`${SMART_LATCH_ESP_API}${endpoint}`)
		.then((res) => res.json())
		.catch((err) => err);
};

exports.smartLatchPost = (endpoint = "/", data = {}) => {
	return fetch(`${SMART_LATCH_ESP_API}${endpoint}`, {
		method: "POST", // *GET, POST, PUT, DELETE, etc.
		mode: "cors", // no-cors, *cors, same-origin
		cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
		credentials: "same-origin", // include, *same-origin, omit
		headers: {
			"Content-Type": "application/json",
			// 'Content-Type': 'application/x-www-form-urlencoded',
		},
		redirect: "follow", // manual, *follow, error
		referrerPolicy: "no-referrer", // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
		body: JSON.stringify(data), // body data type must match "Content-Type" header
	});
};
