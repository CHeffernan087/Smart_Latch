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

exports.verifyUser = async (req, res) => {
	const client = new OAuth2Client(APP_GOOGLE_CLIENT_ID);
	const token = req.query && req.query.idToken;

	let payload = null;
	if (!token) {
		res.send({ error: "No 'idToken' parameter provided." });
		return;
	}

	async function verify() {
		const ticket = await client.verifyIdToken({
			idToken: token,
			audience: APP_GOOGLE_CLIENT_ID,
		});

		payload = ticket.getPayload();
		return payload;
	}
	verify()
		.then(({ given_name, family_name, email, sub }) => {
			checkShouldCreateAccount(email).then((newUser) => {
				const refreshToken = randtoken.uid(256);
				if (newUser) {
					// create account
					readInJwtSecret().then(() => {
						const token = jwt.sign(
							{ email, firstName: given_name, lastName: family_name, id: sub },
							jwt_secret
						);
						registerAsUser(email, given_name, family_name, sub, refreshToken)
							.then(() => {
								addRefreshToken(email, refreshToken);
							})
							.then(() => {
								res.send({
									success: true,
									newAccount: false,
									token,
									refreshToken,
								});
							});
					});
				} else {
					//log in
					// need to implement JWT generation and signing
					readInJwtSecret()
						.then(() => {
							addRefreshToken(email, refreshToken);
						})
						.then(() => {
							const token = jwt.sign(
								{
									email,
									firstName: given_name,
									lastName: family_name,
									id: sub,
								},
								jwt_secret
							);
							res.send({
								success: true,
								newAccount: false,
								token,
								refreshToken,
							});
						});
				}
			});
		})
		.catch((e) => {
			console.log(e);
			res.send({ success: false, error: "Token failed verification." });
		});
};
