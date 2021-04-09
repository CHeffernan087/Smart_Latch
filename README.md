# Smart_Latch
IoT project to securely open a door using a combination of NFC technology and facial recognition.

## Introduction
This file outlines an overview of our Smart Latch IoT system, discussed with specific consideration given to aspects of scalability, security, power and capability for OTA updates. The system is based on the concept of two-factor authentication (2FA) using both NFC and facial recognition technology to provide secure and authorised access to a door. An Android application is used to handle NFC interactions along with user and 'latch' management. An ESP-EYE module provides the facial recognition mechanism with a camera feed, while the latch itself is controlled by an ESP32 board along with a servo motor.  

## Scalability  
Todo: 
- Mention that app is by nature easily scaled
- New latches (esps) can be added pretty easily to DB 
- Talk about load balancer, redis stuff --> can cater to endless amt. of users 
- Cloud func.s are scalable
- How are the web sockets made scalable 

## Security  
As a core consideration of the project, security is upheld to as high a standard as possible in each component of the system. Regarding the app, all communications take place over the `HTTPS` protocol. The user is signed in using the Google OAuth flow, verifying their identity through their Google account. Furthermore, all requests made to the app's back end utilize a token-based authentication scheme, where JWTs are used to authorize all of the user's requests to our back end. This complies with the current best security standards for smartphone applications. In this manner, the back end cloud functions can only be executed by an authorized user, making our back end completely secure. Regarding storage of user data, Google's Firebase is used, drawing on this service's standrard security measures to protect our user data. 

- need to speak more about backend and esp side security. (web sock)
- ESPNOw protocol etc. 

## Power Consumption

## OTA 

## System Overview 

## Running the code 

