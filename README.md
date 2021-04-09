# Smart_Latch
IoT project to securely open a door using a combination of NFC technology and facial recognition.

## Introduction
This file outlines an overview of our Smart Latch IoT system, discussed with specific consideration given to aspects of scalability, security, power consumption and capability for OTA updates. The system is based on the concept of two-factor authentication (2FA) using both NFC and facial recognition technology to provide secure and authorised access to a latch (door). An Android application is used to handle NFC interactions along with user and door management. An ESP-EYE module provides the facial recognition mechanism with a camera feed, while the door itself is controlled by an ESP32 board along with a servo motor.  

## System Overview 
Our system is composed of 3 main components, those being the app, the ESP boards and lastly back end services. As can be observed below, the back end is comprised of multiple sub-components to enhance its scalability. 

![system_overview](https://user-images.githubusercontent.com/44208016/114194625-9bc46800-9947-11eb-9138-f8816b9a61e4.png)

#### App
The Android application manages users and door information, allowing users to view and interact with their doors. The app is also responsible for authentication by way of NFC. It includes some other functionality, like capability to upload a selfie to be used by the facial recognition system. 

#### Back end
The app interacts with serverless Google Cloud functions, which enable it to interact with a Firebase store, which stores user and door information. Other servers take the form of Google Virtual Machines (VM) and are used to communicate with ESP32 boards over websocket (WS) connection. Referencing the above architecture diagram, interactions between these services are made scalable by use of a Redis server and load balancer, which manages scalable interactions between the ESP boards and cluster of VMs. A Python Flask server is used to host the facial recognition service, which carries out facial recognition on images submitted to it. 

#### ESP boards 
Two ESP boards provide the hardware capability underlying the project. The first is an ESP32 board, which interacts with a VM server over WS connection, and controls the actual latch mechanism using a servo motor. This board is primarily responsible for checking if both aspects of authentication (NFC and facial recognition) have succeeded. An ESP-EYE module is used to provide the facial recognition system with input image feed when a subject is onfront of a Smart Latch.

## Scalability  
Since Smart Latch is a solution to less scalable solutions, (keycard access, traditional key etc.) scalability was a primary concern of the project's implementation. Smartphone applications by their nature are scalable in that they can be easily distributed, however more work was required when ensuring our back end and hardware systems remained scalable. Serverless endpoints are created for the app to interface with the back end services using Google cloud functions, which allow new instances of the function to be deployed as traffic increases. On the other side of the infrastructre, communications between the ESP module and Google VM cloud server component occur over a websocket (WS) connection. As Web sockets are a persistent connection, it is crucial that they are scalable. Our architecture supports a cluster of virtual machines which will scale to support any amount of websocket connections automatically. A load balancer sits on front ofthe VM cluster and directs WS traffic to WS servers in the cluster. The individual websokcet servers are synchroized using a redis publisher subscriber architecture. Our cloud can therefore be scaled to support an infinite amount of users. This ensures there is no 'bottlenecks' in the system's ability to scale.

## Security  
As a core consideration of the project, security is upheld to as high a standard as possible in each component of the system. Regarding the app, all communications take place over the `HTTPS` protocol. The user is signed in using the Google OAuth flow, verifying their identity through their Google account. Furthermore, all requests made to the app's back end utilize a token-based authentication scheme, where JWTs are used to authorize all of the user's requests to our back end. This complies with the current best security standards for smartphone applications. In this manner, the back end cloud functions can only be executed by an authorized user, making our back end completely secure. Regarding storage of user data, Google's Firebase is used, drawing on this service's standrard security measures to protect our user data. 

The nature of 2FA and the structure of our system means that the ESP board interfaces with two entirely different services to perform authentication. This decentralisation of the two aspects of authentication means that a malicious entity would need to take control of two servers to perform an attack. This adds an extra security layer to the system. Furthermore, the previously described efforts to increase scalability with a load balancer and also close WS after a timeout provides measures against attacks like DDoS. The Redis server is also only accessible from a virtual private connection (VPC) and thus is only internally accessible. 

*perhaps people have more to add here - I'm trying to keep things brief*

## Power Consumption

## OTA 

## Running the code 
### App
Android Studio is required to run the app. Simply open the `client-app` project in Android Studio. From here the app can be run on an emulator or an Android phone connected over USB. 

