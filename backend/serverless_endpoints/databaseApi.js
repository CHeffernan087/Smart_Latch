const admin = require("firebase-admin");
admin.initializeApp();
const firestoreDb = admin.firestore();

const Firestore = require("@google-cloud/firestore");
// Comment out if not running locally and uncomment above line 3 ---->
// const firestoreDb = new Firestore({
// 	projectId: "smart-latch",
// 	keyFilename: "../../smart-latch-3f77ccdb8958.json",
// });
// <------------

const NFC_RESET_INTERVAL = 20000;

function addAsAuthorised(email, doorId) {
	doorDocument = firestoreDb.collection("Doors").doc(doorId);
	userDoc = firestoreDb.collection("Users").doc(email);
	return doorDocument.update({
		Authorised: admin.firestore.FieldValue.arrayUnion(userDoc),
	});
}

exports.addDoorToUser = (email, doorId) => {
	doorDocument = firestoreDb.collection("Doors").doc(doorId);
	userDoc = firestoreDb
		.collection("Users")
		.doc(email)
		.collection("Doors")
		.doc(doorId);
	return userDoc.set({ doorObj: doorDocument });
};

exports.addRefreshToken = (email, refreshToken) => {
	const docRef = firestoreDb
		.collection("Users")
		.doc(email)
		.collection("RefreshToken")
		.doc(refreshToken);

	const tempDate = new Date(Date.now());
	const issuedAt = new Date(Date.now());
	const expiresAt = new Date(tempDate.setMonth(tempDate.getMonth() + 6));

	return docRef.set({
		issuedAt: issuedAt.valueOf(),
		expiresAt: expiresAt.valueOf(),
		refreshToken,
	});
};

exports.checkShouldCreateAccount = (email) => {
	return exports.queryUserInDB(email).then((userExists) => !userExists);
};

exports.deleteUserFromDB = (email) => {
	return firestoreDb.collection("Users").doc(email).delete();
};

exports.isAuthorised = async (email, doorId) => {
	console.log("Checking the authorisation ");
	let userAuthed = false;
	userDoc = firestoreDb.collection("Users").doc(email);
	doors = firestoreDb.collection("Doors");
	const doorsAuthorisedForUser = (
		await doors.where("Authorised", "array-contains", userDoc).get()
	).docs;
	for (let index = 0; index < doorsAuthorisedForUser.length; index++) {
		const doorDocument = doorsAuthorisedForUser[index];
		if (doorDocument.id == doorId) {
			userAuthed = true;
		}
	}
	console.log("finished checking the authorisation");
	return new Promise((res) => res(userAuthed));
};

exports.isDoorActive = async (doorId) => {
	return (await firestoreDb.collection("Doors").doc(doorId).get()).get(
		"IsActive"
	);
};

exports.getUserDoors = (email) => {
	return firestoreDb
		.collection("Users")
		.doc(email)
		.collection("Doors")
		.get()
		.then((doorCollection) => {
			const doors = [];
			doorCollection.forEach((doc) => {
				doors.unshift(doc.id);
			});
			return doors;
		});
};

exports.getUserRefreshToken = (email, refreshToken) => {
	console.log(`RefreshToken: ${refreshToken}`);
	return firestoreDb
		.collection("Users")
		.doc(email)
		.collection("RefreshToken")
		.doc(refreshToken)
		.get()
		.then((rawJson) => rawJson.data())
		.then((refreshObj) => {
			return refreshObj.refreshToken === refreshToken;
		});
};

exports.getUserDetails = (email) => {
	return firestoreDb
		.collection("Users")
		.doc(email)
		.get()
		.then((details) => {
			return details;
		});
};

exports.queryUserInDB = (email) => {
	return firestoreDb
		.collection("Users")
		.doc(email)
		.get()
		.then((user) => user.exists);
};

exports.registerAsUser = (email, firstname, lastname, userId) => {
	const docRef = firestoreDb.collection("Users").doc(email);
	return docRef.set({
		firstname: firstname,
		lastname: lastname,
		email: email,
		userId,
	});
};

exports.revokeToken = (email, refreshToken) => {
	return firestoreDb
		.collection("Users")
		.doc(email)
		.collection("RefreshToken")
		.doc(refreshToken)
		.delete();
};

exports.setDoorAdmin = async (email, doorId) => {
	doorDocument = firestoreDb.collection("Doors").doc(doorId);
	userDoc = firestoreDb.collection("Users").doc(email);
	await setDoorAsActive(doorId);
	await addAsAuthorised(email, doorId);
	return doorDocument.update({ Admin: userDoc });
};

const getDoor = (doorId) => firestoreDb.collection("Doors").doc(doorId);

exports.getDoorDetails = (doorId) => {
	return firestoreDb
		.collection("Doors")
		.doc(doorId)
		.get()
		.then((rawJson) => {
			return rawJson.data();
		});
};

exports.setDoorNfcId = (doorId, nfcID) => {
	let doorDoc = firestoreDb.collection("Doors").doc(doorId);
	return doorDoc.update({ nfcId: nfcID });
};

exports.toggleLockState = (doorId) => {
	return firestoreDb
		.collection("Doors")
		.doc(doorId)
		.get()
		.then((doc) => {
			if (doc.exists) {
				return doc.ref.update({ locked: !doc.data().locked });
			}
		});
};

exports.updateDoorNfcState = (doorId) => {
	const timer = setTimeout(
		() => resetNfcAfterTimeout(doorId),
		NFC_RESET_INTERVAL
	);
	return firestoreDb.collection("Doors").doc(doorId).update({
		nfcState: true,
	});
};

exports.setLockState = (doorId, isLocked) => {
	doorDocument = firestoreDb.collection("Doors").doc(doorId);
	return doorDocument.update({ locked: isLocked });
};

function resetNfcAfterTimeout(doorId) {
	firestoreDb.collection("Doors").doc(doorId).update({
		nfcState: false,
	});
}

function setDoorAsActive(doorId) {
	doorDocument = firestoreDb.collection("Doors").doc(doorId);
	return doorDocument.update({ IsActive: true });
}
