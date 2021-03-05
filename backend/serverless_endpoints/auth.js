const { SecretManagerServiceClient } = require("@google-cloud/secret-manager");
const { OAuth2Client } = require("google-auth-library");
const {
	checkShouldCreateAccount,
	registerAsUser,
	addRefreshToken,
} = require("./databaseApi");
let randtoken = require("rand-token");
const jwt = require("jsonwebtoken");

const APP_GOOGLE_CLIENT_ID =
	"639400548732-9ga9sg95ao0drj5sdtd3v561adjqptbr.apps.googleusercontent.com";
const client = new SecretManagerServiceClient();
let jwt_secret;

async function readInJwtSecret() {
	const [data] = await client.accessSecretVersion({
		name: "projects/639400548732/secrets/SMART_LATCH_SECRET/versions/latest",
	});
	jwt_secret = data.payload.data.toString();
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

const verify = async (token) => {
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

	verify(token)
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
