exports.home = (app) => {
	app.get("/", (req, res, next) => {
		next("Error: Not found. Please specify a valid endpoint");
	});
};
