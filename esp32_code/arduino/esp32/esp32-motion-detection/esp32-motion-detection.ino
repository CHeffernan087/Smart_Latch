/*
  Rui Santos
  Complete project details at https://RandomNerdTutorials.com/esp-now-esp32-arduino-ide/
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files.
  
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
*/


#include <esp_now.h>
#include <esp_wifi.h>
#include <WiFi.h>
#include "OTA.h"

#define MOTION_PIN 32

// RECEIVER MAC Address
// All F's sends to all boards on network
uint8_t broadcastAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

// Specific to McNally's Boards
//uint8_t broadcastAddress[] = {0xAC, 0x67, 0xB2, 0x67, 0xA1, 0x9C};
//uint8_t broadcastAddress[] = {0x24, 0x0A, 0xC4, 0xE7, 0xF7, 0x94};

// Structure of send data
typedef struct struct_message {
  char a[32];
  String b;
  bool c;
} struct_message;

// Create a struct_message called myData
struct_message myData;


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
  Serial.print("\r\nLast Packet Send Status:\t");
  Serial.println(status == ESP_NOW_SEND_SUCCESS ? "Delivery Success" : "Delivery Fail");
}

// Over-the-air (OTA) updates
OTA ota_updater("1.1", "esp32", "https://europe-west2-smart-latch.cloudfunctions.net/getDownloadUrl");


void setup() {
  // Init Serial Monitor
  Serial.begin(115200);

  // Input motion pin
  pinMode(MOTION_PIN, INPUT);
  
  // Set device as a Wi-Fi Station
  WiFi.mode(WIFI_STA);
  WiFi.begin(); // this connects to the last used WiFi network
//  WiFi.begin(ssid, password);

  // Get WIFI channel
  int32_t channel = getWiFiChannel(WiFi.SSID().c_str());

  // Set WIFI channel
  WiFi.printDiag(Serial); // Uncomment to verify channel number before
  esp_wifi_set_promiscuous(true);
  esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
  esp_wifi_set_promiscuous(false);
  WiFi.printDiag(Serial); // Uncomment to verify channel change after

  // Init ESP-NOW protocol
  if (esp_now_init() != ESP_OK) {
    Serial.println("Error initializing ESP-NOW");
    return;
  }

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
   // check if updates are avilable and download if so
  ota_updater.checkForUpdates();
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
      Serial.println("Sent with success");
    }
    else {
      Serial.println("Error sending the data");
    }

    // delay to prevent spamming
    delay(2000);
  }
}
