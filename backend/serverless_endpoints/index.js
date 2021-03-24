const auth = require("./auth");
const doors = require("./doors");
const user = require("./user");
const { authed } = require("./middleware/auth");

exports.deleteUser = user.deleteUser;
exports.getOldToken = auth.getOldToken;
exports.getUserDoors = authed(doors.getUserDoors);
exports.logout = authed(auth.logout);
exports.testAuthMiddleware = authed(auth.testAuthMiddleware);
exports.toggleLatch = authed(doors.toggleLatch);
exports.registerDoor = authed(doors.registerDoor);
exports.registerUser = user.registerUser;
exports.refreshToken = auth.refreshToken;
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
