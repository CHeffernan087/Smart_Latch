const auth = require("./auth");
const doors = require("./doors");
const user = require("./user");
const { authed } = require("./middleware/auth");

exports.deleteUser = user.deleteUser;
exports.getUserDoors = authed(doors.getUserDoors);
exports.logout = authed(auth.logout);
exports.registerDoor = authed(doors.registerDoor);
exports.registerUser = user.registerUser;
exports.testAuthMiddleware = authed(auth.testAuthMiddleware);
exports.toggleLatch = authed(doors.toggleLatch);
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
exports.refreshToken = auth.refreshToken;
exports.getOldToken = auth.getOldToken;
