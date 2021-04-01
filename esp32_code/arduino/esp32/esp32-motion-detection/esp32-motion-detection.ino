#include <esp_now.h>
#include <esp_wifi.h>
#include <WiFi.h>
#include <Servo.h>

#define MOTION_PIN 32
#define GREEN_LED  33
#define RED_LED    26
#define SERVO_PIN  18
#define DOOR_BTN   19

// timeouts
#define LATCH_TIMEOUT 10000

// create servo object to control a servo
Servo myservo;

const char* ssid = "McNallys";
const char* password = "mcnally123";

// RECEIVER MAC Address
// All F's sends to all boards on network
uint8_t broadcastAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

// Structure of send data
typedef struct struct_message {
  char a[32];
  String b;
  bool c;
} struct_message;

// Create a struct_message called myData
struct_message myData;

// timeout variables
unsigned long openStartTime  = millis();
unsigned long openCurrTime   = millis();

// door state vars
bool currDoorBtnState = false;
bool prevDoorBtnState = false;
bool latchOpen = false;

// Gets WIFI channel using WIFI SSID
int32_t getWiFiChannel(const char *ssid) {
  if (int32_t n = WiFi.scanNetworks()) {
    for (uint8_t i=0; i<n; i++) {
      if (!strcmp(ssid, WiFi.SSID(i).c_str())) {
        return WiFi.channel(i);
      }
    }
  }
  return 0;
}

// Callback when data is sent
void OnDataSent(const uint8_t *mac_addr, esp_now_send_status_t status) {
  Serial.print("[ESP-NOW] Last Packet Send Status:\t");
  Serial.println(status == ESP_NOW_SEND_SUCCESS ? "Delivery Success" : "Delivery Fail");
}


// callback function that will be executed when data is received
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len){
  memcpy(&myData, incomingData, sizeof(myData));
  Serial.print("[ESP-NOW] Packet Recieved:");
  Serial.print("Bytes received: ");
  Serial.println(len);
  Serial.print("Char: ");
  Serial.println(myData.a);
  Serial.print("String: ");
  Serial.println(myData.b);
  Serial.print("Bool: ");
  Serial.println(myData.c);
  Serial.println();
  
  // if string is toggle and bool is true
  if(myData.c && (myData.b == "toggle")){
    Serial.println("[ESP-NOW] Request to Open Latch\n");
    openLatch();
  }
}


// close the latch
void closeLatch(void) {
  digitalWrite(GREEN_LED, LOW);
  digitalWrite(RED_LED, HIGH);
  myservo.write(180);
  latchOpen = false;
  // Set motion values to send
  strcpy(myData.a, "ESP-32 -> ESP-EYE");
  myData.b = "latch";
  myData.c = latchOpen;
  
  // Send message via ESP-NOW
  esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *) &myData, sizeof(myData));
  Serial.print("[ESP-NOW] esp-eye -> esp32: sending string - ");
  Serial.println(myData.b);
  
  if (result == ESP_OK) {
    Serial.println("[ESP-NOW] Sent with success");
  }
  else {
    Serial.println("[ESP-NOW] Error sending the data");
  }
}

// open the latch
void openLatch(void) {
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(RED_LED, LOW);
  myservo.write(0);
  latchOpen = true;
  openStartTime  = millis();
  // Set motion values to send
  strcpy(myData.a, "ESP-32 -> ESP-EYE");
  myData.b = "latch";
  myData.c = latchOpen;
  
  // Send message via ESP-NOW
  esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *) &myData, sizeof(myData));
  Serial.print("[ESP-NOW] esp-eye -> esp32: sending string - ");
  Serial.println(myData.b);
  
  if (result == ESP_OK) {
    Serial.println("[ESP-NOW] Sent with success");
  }
  else {
    Serial.println("[ESP-NOW] Error sending the data");
  }
}


void setup() {
  // Init Serial Monitor
  Serial.begin(115200);

  // Input motion pin
  pinMode(MOTION_PIN, INPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(SERVO_PIN, OUTPUT);
  pinMode(DOOR_BTN, INPUT);

  // config servo pin
  myservo.attach(SERVO_PIN);


  // Set device as a Wi-Fi Station
  WiFi.mode(WIFI_STA);

  // Get WIFI channel
  int32_t channel = getWiFiChannel(ssid);

  // Set WIFI channel
  WiFi.printDiag(Serial); // Uncomment to verify channel number before
  esp_wifi_set_promiscuous(true);
  esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
  esp_wifi_set_promiscuous(false);
  WiFi.printDiag(Serial); // Uncomment to verify channel change after

  // Init ESP-NOW protocol
  if (esp_now_init() != ESP_OK) {
    Serial.println("[ESP-NOW] Error initializing ESP-NOW");
    return;
  }

  // Once ESPNow is successfully Init, we will register for recv CB to
  // get recv packer info
  esp_now_register_recv_cb(OnDataRecv);


  // Once ESPNow is successfully Init, we will register for Send CB to
  // get the status of Trasnmitted packet
  esp_now_register_send_cb(OnDataSent);
  
  // Register peer device
  esp_now_peer_info_t peerInfo;
  memcpy(peerInfo.peer_addr, broadcastAddress, 6);
  peerInfo.channel = 0;  
  peerInfo.encrypt = false;
  
  // Add peer device       
  if (esp_now_add_peer(&peerInfo) != ESP_OK){
    Serial.println("Failed to add peer");
    return;
  }

  closeLatch();
}
 
void loop() {

  // if motion has been detected
  if(digitalRead(MOTION_PIN)){
    
    // Set motion values to send
    strcpy(myData.a, "ESP32 -> ESP-EYE");
    myData.b = "motion";
    myData.c = true;
  
    // Send message via ESP-NOW
    esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *) &myData, sizeof(myData));
     
    if (result == ESP_OK) {
      Serial.println("[ESP-NOW] Sent with success");
    }
    else {
      Serial.println("[ESP-NOW] Error sending the data");
    }
    delay(2000);
  }

  currDoorBtnState = digitalRead(DOOR_BTN);
  
  // open latch timeout
  // latch will always close after 10 seconds if the door is still closed   
  openCurrTime = millis();
  if ((openCurrTime - openStartTime > LATCH_TIMEOUT) && currDoorBtnState && latchOpen){
    Serial.println("[LATCH] Timeout - Door did not Open");
    closeLatch();
  }

  // detect a positive edge on the door button
  // if button is pressed but was not during previous iteration the door has been closed
  // so we automatically close the latch
  if (currDoorBtnState && !prevDoorBtnState && latchOpen){
    Serial.println("[LATCH] Door Closed");
    closeLatch();
  }
  
  // update previous door button state for next loop
  prevDoorBtnState = digitalRead(DOOR_BTN);

  delay(500);
}
