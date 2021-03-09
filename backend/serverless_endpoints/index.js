const auth = require("./auth");
const doors = require("./doors");
const user = require("./user");
const { authed } = require("./middleware/auth");

exports.deleteUser = user.deleteUser;
exports.getUserDoors = doors.getUserDoors;
exports.logout = authed(auth.logout);
exports.registerDoor = doors.registerDoor;
exports.registerUser = user.registerUser;
exports.testAuthMiddleware = authed(auth.testAuthMiddleware);
exports.toggleLatch = doors.toggleLatch;
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
