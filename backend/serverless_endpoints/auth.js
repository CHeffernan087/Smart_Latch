const { OAuth2Client } = require("google-auth-library");
const {
	addRefreshToken,
	checkShouldCreateAccount,
	registerAsUser,
	getUserDetails,
	getUserRefreshToken,
	revokeToken,
} = require("./databaseApi");
const { getUser, isRequestAllowed, readInJwtSecret } = require("./utils");
let randtoken = require("rand-token");
const jwt = require("jsonwebtoken");
const { firestore } = require("firebase-admin");

const APP_GOOGLE_CLIENT_ID =
	"639400548732-9ga9sg95ao0drj5sdtd3v561adjqptbr.apps.googleusercontent.com";

const logUserIn = ({ given_name, family_name, email, sub }, newUser) => {
	const refreshToken = randtoken.uid(256);
	return new Promise((resolve) => {
		if (newUser) {
			registerAsUser(
				email,
				given_name,
				family_name,
				sub,
				refreshToken
			).then(() => resolve());
		} else {
			resolve();
		}
	})
		.then(() => {
			return addRefreshToken(email, refreshToken);
		})
		.then(readInJwtSecret)
		.then((jwt_secret) => {
			const token = jwt.sign(
				{ email: email, firstName: given_name, lastName: family_name, id: sub },
				jwt_secret,
				{ expiresIn: "12h" }
			);
			return {
				success: true,
				newAccount: false,
				token,
				refreshToken,
			};
		});
};

exports.logout = (req, res) => {
	if (!isRequestAllowed(req, "POST")) {
		return res.status(400).send({ error: "Expected request type POST" });
	}
	
	const { email } = req.query;
	const { refreshToken } = req.query;
	if (!refreshToken) {
		return res.status(400).send({
			error:
				"Error. Cannot logout. Attach the refresh token you would like to revoke in the body of the request",
		});
	}
	revokeToken(email, refreshToken)
		.then(() => {
			return res.status(200).send({ message: "User logged out" });
		})
		.catch(() => {
			return res.status(400).send({
				error:
					"Error. Cannot logout. Attach the refresh token you would like to revoke in the body of the request",
			});
		});
};

exports.testAuthMiddleware = (req, res) => {
	res
		.status(200)
		.send({ message: "Your token has been succesfully verified!" });
};

const verifyToken = async (token) => {
	const client = new OAuth2Client(APP_GOOGLE_CLIENT_ID);
	const ticket = await client.verifyIdToken({
		idToken: token,
		audience: APP_GOOGLE_CLIENT_ID,
	});

	payload = ticket.getPayload();
	return payload;
};

exports.verifyUser = async (req, res) => {
	const token = req.query && req.query.idToken;

	if (!token) {
		return res.send({ error: "No 'idToken' parameter provided." });
	}

	verifyToken(token)
		.then(({ email, ...userCredentials }) => {
			return checkShouldCreateAccount(email).then((newUser) => {
				return { newUser, ...userCredentials, email };
			});
		})
		.then(({ newUser, ...userCredentials }) => {
			return logUserIn({ ...userCredentials }, newUser);
		})
		.then((responsePayload) => {
			return res.status(200).send(responsePayload);
		})
		.catch((e) => {
			res.send({ success: false, error: "Token failed verification." });
		});
};

exports.getOldToken = async (req, res) => { // endpoint can be used to get old tokens for testing 
	const email = req.query && req.query.email;
	const issuedAt = new Date();
	issuedAt.setDate(issuedAt.getDate() - 2) // 2 days ago
	Math.floor(Date.now() / 1000)
	const oldTS = Math.floor(issuedAt / 1000)

	readInJwtSecret()
				.then((jwt_secret) => {
					getUserDetails(email)
						.then((details) => {
							const firstName = details._fieldsProto.firstname.stringValue;
							const lastName = details._fieldsProto.lastname.stringValue;
							const userId = details._fieldsProto.userId.stringValue;
				
							const token = jwt.sign(
								{ email: email, firstName: firstName, lastName: lastName, id: userId, iat: oldTS, exp: oldTS+3600},
								jwt_secret
							);
		
							res.send({token: token}).status(200);
						}).catch((e) => {
							console.log(e);
							res.send({error: "Error accessing user details."}).status(400);
						});
				}).catch((e) => console.log(e));

}

exports.refreshToken = async (req, res) => {
	console.log(`Refresh token endpoint`);
	const refreshToken  = req.query && req.query.refreshToken; 
	const email = req.query && req.query.email;
	if (!refreshToken) {
		res.send({ message: "Must include refreshToken parameter."}).status(400);
	} else if (!email) {
		res.send({ message: "Must include email parameter."}).status(400);
	}
	
	getUserRefreshToken(email, refreshToken)
		.then((verifyRefreshToken) => {
			console.log(`Refresh token is: ${verifyRefreshToken}`);
			if (!verifyRefreshToken) {
				res.send({
					verifiedRefreshToken: verifyRefreshToken, 
					message: "Refresh token failed verification. Please re-login."
				}).status(401);
				return; 
			}
		})
		.then(() => {
			const issuedAt = Math.floor(Date.now() / 1000)
	
			readInJwtSecret()
				.then((jwt_secret) => {
					getUserDetails(email)
						.then((details) => {
							const firstName = details._fieldsProto.firstname.stringValue;
							const lastName = details._fieldsProto.lastname.stringValue;
							const userId = details._fieldsProto.userId.stringValue;

							const token = jwt.sign(
								{ email: email, firstName: firstName, lastName: lastName, id: userId, iat: issuedAt, exp: issuedAt + 43200}, // persist for 12 hours
								jwt_secret
							);
		
							res.send({token: token}).status(200);
						}).catch((e) => {
							res.send({error: `Error accessing user details: ${e}`}).status(401);
						});
				}).catch((e) => {
					res.send({ message: `Error accessing JWT secret: ${e}`}).status(401);
				});
		});	
};
