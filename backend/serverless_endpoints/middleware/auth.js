const jwt = require("jsonwebtoken");
const { readInJwtSecret } = require("./utils");

module.exports = (req, res, next) => {
	const token = req.header("x-auth-token");
	if (!token)
		return res.status(401).send({ error: "Access denied. No token provided." });

	readInJwtSecret()
		.then((jwtSecret) => {
			const payload = jwt.verify(token, jwtSecret);
			req.user = payload;
			next();
		})
		.catch((err) => {
			res.status(400).send({ error: "Invalid token." });
		});
};
