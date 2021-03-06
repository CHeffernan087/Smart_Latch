const auth = require("./auth");
const doors = require("./doors");
const user = require("./user");

exports.deleteUser = user.deleteUser;
exports.getUserDoors = doors.getUserDoors;
exports.registerDoor = doors.registerDoor;
exports.registerUser = user.registerUser;
exports.toggleLatch = doors.toggleLatch;
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
exports.refreshToken = auth.refreshToken;
