const auth = require("./auth");
const doors = require("./doors");
const user = require("./user");

const admin = require("firebase-admin");

admin.initializeApp();

exports.deleteUser = user.deleteUser;
exports.getUserDoors = doors.getUserDoors;
exports.registerDoor = doors.registerDoor;
exports.registerUser = user.registerUser;
exports.toggleLatch = doors.toggleLatch;
exports.userExists = user.userExists;
exports.verifyUser = auth.verifyUser;
