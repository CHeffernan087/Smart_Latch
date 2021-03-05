exports.useMiddleware = (middleware, nextFunction) => {
	return (req, res) => {
		middleware(req, res, nextFunction);
	};
};
