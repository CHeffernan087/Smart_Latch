CREATE DATABASE IF NOT EXISTS SmartLatch;
USE SmartLatch;
CREATE TABLE IF NOT EXISTS Accounts (
    user_id_num INT PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS UserDoors (
    user_id_num INT NOT NULL,
    door_id INT NOT NULL UNIQUE,
    FOREIGN KEY(user_id_num) REFERENCES Accounts(user_id_num)
);