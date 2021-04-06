const auth = require("./auth");
const doors = require("./doors");
const redis = require("./redis");
const user = require("./user");
const { authed } = require("./middleware/auth");

exports.deleteUser = user.deleteUser;
exports.getOldToken = auth.getOldToken;
exports.getUserDoors = authed(doors.getUserDoors);
exports.logout = authed(auth.logout);
exports.nfcUpdate = authed(doors.nfcUpdate);
exports.refreshToken = auth.refreshToken;
exports.registerDoor = authed(doors.registerDoor);
exports.registerUser = user.registerUser;
exports.setLockState = doors.setLockState;
exports.testAuthMiddleware = authed(auth.testAuthMiddleware);
exports.toggleLatch = authed(doors.toggleLatch);
exports.toggleLockState = doors.toggleLockState;
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
