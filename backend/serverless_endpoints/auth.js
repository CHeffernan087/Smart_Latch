const { OAuth2Client } = require("google-auth-library");
const {
	checkShouldCreateAccount,
	registerAsUser,
	addRefreshToken,
} = require("./databaseApi");
const { readInJwtSecret } = require("./utils");
let randtoken = require("rand-token");
const jwt = require("jsonwebtoken");

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
			console.log(e);
			res.send({ success: false, error: "Token failed verification." });
		});
};
