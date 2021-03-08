const { SecretManagerServiceClient } = require("@google-cloud/secret-manager");
const { OAuth2Client } = require("google-auth-library");
const {
	checkShouldCreateAccount,
	registerAsUser,
	addRefreshToken,
	getUserDetails,
	getUserRefreshToken,
} = require("./databaseApi");
let randtoken = require("rand-token");
const jwt = require("jsonwebtoken");

const APP_GOOGLE_CLIENT_ID =
	"639400548732-9ga9sg95ao0drj5sdtd3v561adjqptbr.apps.googleusercontent.com";
const client = new SecretManagerServiceClient();
let jwt_secret;

async function readInJwtSecret() {
	try {
		const [data] = await client.accessSecretVersion({
			name: "projects/639400548732/secrets/SMART_LATCH_SECRET/versions/latest",
		});
		jwt_secret = data.payload.data.toString();
	} catch (e) {
		jwt_secret = "default";
	}
}

const logUserIn = ({ given_name, family_name, email, sub }, newUser) => {
	const refreshToken = randtoken.uid(256);
	return readInJwtSecret()
		.then(() => {
			if (newUser) {
				return registerAsUser(
					email,
					given_name,
					family_name,
					sub,
					refreshToken
				);
			} else {
				return new Promise((res, rej) => res());
			}
		})
		.then(() => {
			return addRefreshToken(email, refreshToken);
		})
		.then(() => {
			const token = jwt.sign(
				{ email: email, firstName: given_name, lastName: family_name, id: sub },
				jwt_secret
			);
			return {
				success: true,
				newAccount: false,
				token,
				refreshToken,
			};
		});
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
			console.log(e);
			res.send({ success: false, error: "Token failed verification." });
		});
};

exports.refreshToken = async (req, res) => {
	console.log(`Refresh token endpoint`);
	const refreshToken  = req.query && req.query.refreshToken; 
	const email = req.query && req.query.email;
	if (!refreshToken) {
		res.send({ message: "Must include refreshToken parameter."}).status(400);
	} else if (!email) {
		res.send({ message: "Must include email parameter."}).status(400);
	}
	// 1. verify the refresh token 
	// const decodedToken = jwt.verify(refreshToken, jwt_secret);
	// console.log(`DECODED TOKEN: ${JSON.stringify(decodedToken)}`);
	getUserRefreshToken(email, refreshToken)
		.then((verifyRefreshToken) => {
			console.log(`Refresh token is: ${verifyRefreshToken}`);
			if (!verifyRefreshToken) {
				res.send({
					verifiedRefreshToken: verifyRefreshToken, 
					message: "Refresh token failed verification."
				}).status(401);
				return; 
			}
		})
		.then(() => {
			const issuedAt = new Date(Date.now());
			readInJwtSecret()
				.then(() => {
					getUserDetails(email)
						.then((details) => {
							const firstName = details._fieldsProto.firstname.stringValue;
							const lastName = details._fieldsProto.lastname.stringValue;
							const userId = details._fieldsProto.userId.stringValue;
				
							const token = jwt.sign(
								{ email: email, firstName: firstName, lastName: lastName, id: userId, iss: issuedAt, exp: 3600},
								jwt_secret
							);
		
							res.send({token: token}).status(200);
						}).catch((e) => {
							console.log(e);
							res.send({error: "Error accessing user details."}).status(400);
						});
				}).catch((e) => console.log(e));
		});	
};
