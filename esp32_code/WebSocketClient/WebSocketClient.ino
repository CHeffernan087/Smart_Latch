
#include <Arduino.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <Servo.h>

// Objects
WebSocketsClient webSocket; // WebSocket object
Servo servo1;               // Servo object

// External Pin Defines
static const int servoPin = 13;   // pin for Servo PWM
static const int pushButton = 32; // pin for Button

// Servo
int servoToggle;               // binary servo position variable
#define ZERO_DEGREES 0         // 0 degrees servo angle
#define ONE_EIGHTY_DEGREES 180 // 180 degrees servo angle

// Network
const char *ssid = "McNallys";       // wifi name
const char *password = "mcnally123"; // wifi password
bool connected = false;              // network connection status

// Timing
unsigned long lastUpdate = millis(); // stores time(ms) since application started
unsigned long messageInterval = 500; // time interval to prevent button bounce

// NOTE: likely to be depricated in the future
// if binary is recieved we print dump in hex form
void hexdump(const void *mem, uint32_t len, uint8_t cols = 16)
{
    const uint8_t *src = (const uint8_t *)mem;
    Serial.printf("\n[HEXDUMP] Address: 0x%08X len: 0x%X (%d)", (ptrdiff_t)src, len, len);
    for (uint32_t i = 0; i < len; i++)
    {
        if (i % cols == 0)
        {
            Serial.printf("\n[0x%08X] 0x%08X: ", (ptrdiff_t)src, i);
        }
        Serial.printf("%02X ", *src);
        src++;
    }
    Serial.printf("\n");
}

// callback for when a response is recieved
void webSocketEvent(WStype_t type, uint8_t *payload, size_t length)
{

    switch (type)
    {
    // if server is disconnecting
    case WStype_DISCONNECTED:
    {
        Serial.printf("[WSc] Disconnected!\n");
        connected = false;
    }
    break;

    // if server is connnecting
    case WStype_CONNECTED:
    {
        Serial.printf("[WSc] Connected to url: %s\n", payload);
        connected = true;

        // send message to server when Connected
        Serial.println("[WSc] SENT: Connected");
        webSocket.sendTXT("Connected");
    }
    break;

    // if a response message is recieved
    // this is where we toggle the servo position
    case WStype_TEXT:
    {
        Serial.printf("[WSc] RESPONSE: %s\n", payload);
        Serial.printf("-> Response Recieved - Toggling Servo ...\n");

        // position the servo
        if (servoToggle)
        {
            servo1.write(ONE_EIGHTY_DEGREES);
        }
        else
        {
            servo1.write(ZERO_DEGREES);
        }

        // toggle the postion indicator
        servoToggle = ~servoToggle;
    }
    break;

    // if binary is receieved from server
    case WStype_BIN:
    {
        Serial.printf("[WSc] get binary length: %u\n", length);

        // dump binary in hex form
        hexdump(payload, length);
    }
    break;

    // if we get a ping
    case WStype_PING:
    {
        // pong will be send automatically
        Serial.printf("[WSc] get ping\n");
    }
    break;

    // if we get a pong
    case WStype_PONG:
    {
        // no action required, answer to a ping we send
        Serial.printf("[WSc] get pong\n");
    }
    break;

    // other possible cases we do nothing
    case WStype_ERROR:
    case WStype_FRAGMENT_TEXT_START:
    case WStype_FRAGMENT_BIN_START:
    case WStype_FRAGMENT:
    case WStype_FRAGMENT_FIN:

        break;
    }
}

// setup run once when application starts
void setup()
{

    // start serial monitor
    Serial.begin(115200);

    // 4 seconds to allow the device to boot fully
    Serial.println("\n\n");
    for (uint8_t t = 4; t > 0; t--)
    {
        Serial.printf("[SETUP] BOOT WAIT %d...\n", t);
        Serial.flush();
        delay(1000);
    }

    pinMode(servoPin, OUTPUT);  // set servo PWM pin to output mode
    pinMode(pushButton, INPUT); // set pushbutton pin to input mode

    servoToggle = 0;         // init servo to position 0
    servo1.attach(servoPin); // set ouptut PWM pin for servo object

    WiFi.begin(ssid, password); // connect device to wifi network

    // poll connection status to ensure connection is made
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }

    // print device IP address
    Serial.print("Local IP: ");
    Serial.println(WiFi.localIP());

    // server address, port and URL
    // just using this websocket service for now to get a default response
    webSocket.begin("smart-latch.herokuapp.com", 80, "/");

    // set websocket event handler
    webSocket.onEvent(webSocketEvent);
}

// loop is basically our embedded while(1)
void loop()
{

    webSocket.loop();

    // if connected, button is pushed, and message interval is reached we send message to server
    // try press and release button in under 500ms, otherwise it will start to spam the server
    if (connected && digitalRead(pushButton) && lastUpdate + messageInterval < millis())
    {
        Serial.println("\n-> Button Pressed - Sending Message ...");
        Serial.println("[WSc] SENT: Simple js client message!!");

        // sending messege to server
        webSocket.sendTXT("Simple js client message!!");
        Serial.println("-> Message Sent - Waiting for Response ...");

        // update time since previous event
        lastUpdate = millis();
    }
}
