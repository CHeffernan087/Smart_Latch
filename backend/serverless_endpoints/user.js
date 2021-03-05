const {
	deleteUserFromDB,
	queryUserInDB,
	registerAsUser,
} = require("./databaseApi");

//curl -d "email=joeblogs@gmail.com" -X DELETE http://localhost:8080/
//curl -d "email=joeblogs@gmail.com" -X DELETE https://europe-west2-smart-latch.cloudfunctions.net/deleteUser --ssl-no-revoke
exports.deleteUser = (req, res) => {
	if (req.method != "DELETE") {
		res.status(400).send({ error: "Needs to be a DELETE request" });
	}
	if (req.body.hasOwnProperty("email") === false) {
		res.status(400).send({ error: "Need to specify email in DELETE" });
	}
	email = req.body.email;
	const userIsAuthorized = false;
	if (userIsAuthorized) {
		deleteUserFromDB(email)
			.then((data) => {
				res.status(200).send({
					message: `Successfully deleted ${email} from DB`,
					data: data,
				});
			})
			.catch((err) => {
				res.status(400).send({ error: err });
			});
	}
};

//curl -d "email=joeblogs@gmail.com&firstname=joe&lastname=blogs" -X POST http://localhost:8080/
//curl -d "email=joeblogs@gmail.com&firstname=joe&lastname=blogs" -X POST https://europe-west2-smart-latch.cloudfunctions.net/registerUser --ssl-no-revoke
exports.registerUser = (req, res) => {
	if (req.method != "POST") {
		res.status(400).send({ error: "Needs to be a POST request" });
	}
	const keys = ["email", "firstname", "lastname"];
	const hasAllKeys = keys.every((key) => req.body.hasOwnProperty(key));
	if (hasAllKeys === false) {
		res.status(400).send({ error: "Missing values in POST request" });
	}
	email = req.body.email;
	firstname = req.body.firstname;
	lastname = req.body.lastname;
	const userIsAuthorized = false;
	if (userIsAuthorized) {
		registerAsUser(email, firstname, lastname)
			.then((data) => {
				res.status(200).send({
					message: `Successfully added ${email}, ${firstname} ${lastname}`,
					data: data,
				});
			})
			.catch((err) => {
				res.status(400).send({ error: err });
			});
	}
};

exports.userExists = (req, res) => {
	queryUserInDB().then((userExists) => {
		res.send({ userExists });
	});
};
