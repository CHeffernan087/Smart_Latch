const fetch = require("node-fetch");
const smartLatchPost = (endpoint = "/", data = {}) => {
	return fetch(`http://localhost:3000/registerDoor`, {
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
		body: JSON.stringify({ doorId: "50" }), // body data type must match "Content-Type" header
	});
};

smartLatchPost()
	.then((res) => res.json())
	.then((data) => {
		console.log(data);
	});
