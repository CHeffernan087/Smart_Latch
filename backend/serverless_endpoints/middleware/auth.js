const jwt = require("jsonwebtoken");
const { useMiddleware } = require("./middlewareUtils");
const { readInJwtSecret } = require("../utils");

const authEndpoint = (req, res, next) => {
	const token = req.header("x-auth-token");
	if (!token)
		return res.status(400).send({ error: "Access denied. No token provided." }); // Bad request, no token.

	readInJwtSecret()
		.then((jwtSecret) => {
			const payload = jwt.verify(token, jwtSecret);
			req.user = payload;
		})
		.catch((err) => {
			res.status(401).send({ error: "Invalid token.", err }); // Unauthorized. 
		})
		.finally(() => {
			next(req, res);
		});
};

exports.authed = (endpointHandler) => {
	return useMiddleware(authEndpoint, endpointHandler);
};
