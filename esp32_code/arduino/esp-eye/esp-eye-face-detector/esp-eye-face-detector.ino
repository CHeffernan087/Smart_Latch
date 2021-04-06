#include <Arduino.h>
#include <esp_now.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WebSocketsClient.h>
#include <base64.h>

#include "esp_timer.h"
#include "esp_camera.h"
#include "camera_index.h"
#include "fd_forward.h"
#include "fr_forward.h"
#include "fr_flash.h"

// Select camera model
#define CAMERA_MODEL_ESP_EYE
#include "camera_pins.h"

// Select camera model
#define CAMERA_MODEL_ESP_EYE

// timeouts
#define MOTION_TIMEOUT     10000
#define NFC_TIMEOUT        10000
#define FACE_RECOG_TIMEOUT 10000

#define CONOR

#if defined(CONOR)
const char* ssid = "McNallys";
const char* password = "mcnally123";
#endif

#if defined(CIARAN)
const char* ssid = "";
const char* password = "";
#endif

// face recognision server POST URL
const char* img_recog_url = "http://recognition.smart-latchxyz.xyz/";
// test POST url
//const char* img_recog_url = "http://httpbin.org/post";

// websocket server URL - health_check: http://smart-latchxyz.xyz/healthcheck/
const char* wss_url = "ws.smart-latchxyz.xyz";
// test WSS url
//const char* wss_url = "echo.websocket.org";

// RECEIVER MAC Address
// All F's sends to all boards on network
uint8_t broadcastAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
//String myMACAddress     = "AC67B267A19C";
String myMACAddress     = "31415";

// websocket
WebSocketsClient webSocket; // websocket object
bool connected = false;     // websocket connection status

// motion detection
bool motionDetected = false;  // motion detection status

// verification flags
bool faceRecogFlag = false;
bool nfcFlag       = false;

// http client
HTTPClient http;
        
// timeout variables
unsigned long mdStartTime  = millis();
unsigned long mdCurrTime   = millis();
unsigned long frStartTime  = millis();
unsigned long frCurrTime   = millis();
unsigned long nfcStartTime = millis();
unsigned long nfcCurrTime  = millis();

// config face detection model parameters
static inline mtmn_config_t app_mtmn_config(){
  mtmn_config_t mtmn_config = {0};
  mtmn_config.type = FAST;
  mtmn_config.min_face = 80;
  mtmn_config.pyramid = 0.707;
  mtmn_config.pyramid_times = 4;
  mtmn_config.p_threshold.score = 0.6;
  mtmn_config.p_threshold.nms = 0.7;
  mtmn_config.p_threshold.candidate_number = 20;
  mtmn_config.r_threshold.score = 0.7;
  mtmn_config.r_threshold.nms = 0.7;
  mtmn_config.r_threshold.candidate_number = 10;
  mtmn_config.o_threshold.score = 0.7;
  mtmn_config.o_threshold.nms = 0.7;
  mtmn_config.o_threshold.candidate_number = 1;
  return mtmn_config;
}
mtmn_config_t mtmn_config = app_mtmn_config();

// structure of send data
typedef struct struct_message {
  char a[32];
  String b;
  bool c;
} struct_message;

// create a struct_message called myData
struct_message myData;

// callback when data is sent
void OnDataSent(const uint8_t *mac_addr, esp_now_send_status_t status){
  Serial.print("[ESP-NOW] Last Packet Send Status:\t");
  Serial.println(status == ESP_NOW_SEND_SUCCESS ? "Delivery Success" : "Delivery Fail");
}

// callback function that will be executed when data is received
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len){
  memcpy(&myData, incomingData, sizeof(myData));
  Serial.print("\n[ESP-NOW] Packet Recieved:\n");
  Serial.print("Bytes received: ");
  Serial.println(len);
  Serial.print("Char: ");
  Serial.println(myData.a);
  Serial.print("String: ");
  Serial.println(myData.b);
  Serial.print("Bool: ");
  Serial.println(myData.c);
  Serial.println();
  
  // if string is motion and bool is true
  if(myData.c && (myData.b == "motion")){
    Serial.println("[ESP-NOW] Motion Detected\n");
    motionDetected = true;
    mdStartTime = millis();
    mdCurrTime  = millis();
  }

  // if string is closed and bool is true
  if(myData.b == "latch"){
    if (myData.c) {
      Serial.println("[ESP-NOW] Latch Opened\n");
      // sending person name over websocket
      Serial.println("[WSc] Notifying Cloud");
      webSocket.sendTXT("opened");
    }
    else {
      Serial.println("[ESP-NOW] Latch Closed\n");
      // sending person name over websocket
      Serial.println("[WSc] Notifying Cloud");
      webSocket.sendTXT("closed");
    }
  }
  
}

// function to change latch state over esp-now
void changeLatchState(void){
  // Set motion values to send
  strcpy(myData.a, "ESP-EYE -> ESP32");
  myData.b = "toggle";
  myData.c = true;
  
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


// NOTE: likely to be depricated in the future
// if binary is recieved we print dump in hex form
void hexdump(const void *mem, uint32_t len, uint8_t cols = 16){
  const uint8_t *src = (const uint8_t *)mem;
  Serial.printf("\n[HEXDUMP] Address: 0x%08X len: 0x%X (%d)", (ptrdiff_t)src, len, len);
  for (uint32_t i = 0; i < len; i++){
    if (i % cols == 0){
        Serial.printf("\n[0x%08X] 0x%08X: ", (ptrdiff_t)src, i);
    }
    Serial.printf("%02X ", *src);
    src++;
  }
  Serial.printf("\n");
}

// callback for when a response is recieved
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
 
  switch(type) {
    case WStype_DISCONNECTED:
      Serial.printf("[WSc] Disconnected!\n");
      connected = false;
      break;
    case WStype_CONNECTED:
      Serial.printf("[WSc] Connected to url: %s\n", payload);
      connected = true;
      // send message to server when Connected
      Serial.println("[WSc] SENT: Connected");
      
      break;
    case WStype_TEXT:            
      Serial.printf("\nResponse Recieved\n");            
      Serial.printf("[WSc] RESPONSE: %s\n", payload);
      if (!memcmp(payload, "ToggleLatch", length)){
        Serial.printf("[WSc] NFC Verified\n", payload);
        nfcFlag = true;
        nfcStartTime = millis();
      }
      if (!memcmp(payload, "boardIdReq", length)){
        Serial.printf("[WSc] Sending Board Id - ");
        Serial.println(myMACAddress);
        String boardIdResp = "message:boardIdRes,doorId:" + myMACAddress;
        webSocket.sendTXT(boardIdResp);
      }
      break;
    case WStype_BIN:
      Serial.printf("[WSc] get binary length: %u\n", length);
      hexdump(payload, length);
      break;
    case WStype_PING:
      // pong will be send automatically
      Serial.printf("[WSc] get ping\n");
      break;
    case WStype_PONG:
      // answer to a ping we send
      Serial.printf("[WSc] get pong\n");
      break;
    case WStype_ERROR:
    case WStype_FRAGMENT_TEXT_START:
    case WStype_FRAGMENT_BIN_START:
    case WStype_FRAGMENT:
    case WStype_FRAGMENT_FIN:
      break;
  }
}
 
void setup() {  
  Serial.begin(115200);
  Serial.println();
  
  // configure camera pins
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  
  //init with high specs to pre-allocate larger buffers
  if (psramFound()) {
    config.frame_size = FRAMESIZE_UXGA;
    config.jpeg_quality = 10; //0-63 lower number means higher quality 
    config.fb_count = 2;
  } else {
    config.frame_size = FRAMESIZE_SVGA;
    config.jpeg_quality = 12; //0-63 lower number means higher quality
    config.fb_count = 1;
  }
  
  #if defined(CAMERA_MODEL_ESP_EYE)
  pinMode(13, INPUT_PULLUP);
  pinMode(14, INPUT_PULLUP);
  #endif
  
  // camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }

  // set frame size
  sensor_t * s = esp_camera_sensor_get();
  s->set_framesize(s, FRAMESIZE_QVGA);
  
  // init boot process
  for(uint8_t t = 4; t > 0; t--) {
    Serial.printf("[SETUP] BOOT WAIT %d...\n", t);
    Serial.flush();
    delay(1000);
  }


  // Set the device as a Station and Soft Access Point simultaneously
  WiFi.mode(WIFI_AP_STA);
  
  // Set device as a Wi-Fi Station
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("[WiFi] Setting as a Wi-Fi Station..");
  }
  Serial.print("[WiFi] Station IP Address: ");
  Serial.println(WiFi.localIP());
  Serial.print("[WiFi] Wi-Fi Channel: ");
  Serial.println(WiFi.channel());
  Serial.print("[WiFi] Board MAC Address: ");
  Serial.println(WiFi.macAddress());
  
  myMACAddress = WiFi.macAddress();
  
  // Init ESP-NOW
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

  // connect to wifi
  WiFi.begin(ssid, password);

  // connection delay
  while ( WiFi.status() != WL_CONNECTED ) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  
  // server address, port and URL
  // just using this websocket service for now to get a default response
  webSocket.beginSSL(wss_url, 443);

  // websocket event handler
  webSocket.onEvent(webSocketEvent);
   
  // connection delay
  delay(2000);
}


size_t frame_num = 0;                 // init frame number
camera_fb_t *fb = NULL;               // allocate image mem
dl_matrix3du_t *image_matrix = NULL;  // allocate image matrix RGB mem

void loop() {

  // websocket object update must be called every loop
  webSocket.loop();

  // if motion is detected we begin verification flow for face recognision
  if (connected && motionDetected){

    // get current time
    int64_t start_time = esp_timer_get_time();
    
    // get one image with camera
    fb = esp_camera_fb_get();
    // if image was not retrieved
    if (!fb){
      Serial.println("[ESP-CAM] Camera capture failed");
      return;
    }

    // get time again
    int64_t fb_get_time = esp_timer_get_time();
    Serial.printf("[ESP-CAM] Get one frame in %lld ms.\n", (fb_get_time - start_time) / 1000);
    
    // allocate image matrix to store RGB data
    image_matrix = dl_matrix3du_alloc(1, fb->width, fb->height, 3);
    
    // transform image to RGB
    uint32_t res = fmt2rgb888(fb->buf, fb->len, fb->format, image_matrix->item);
    // could not transfrom to RGB
    if (true != res){
      Serial.printf("[ESP-CAM] fmt2rgb888 failed, fb: %d\n", fb->len);
      dl_matrix3du_free(image_matrix);
      return;
    }

    // halt camera
    esp_camera_fb_return(fb);
    
    // do face detection
    box_array_t *net_boxes = face_detect(image_matrix, &mtmn_config);
    // print detection time in ms
    Serial.printf("[ESP-CAM] Detection time consumption: %lldms\n", (esp_timer_get_time() - fb_get_time) / 1000);        

    // if face was detected, boxes would be generated
    if (net_boxes){
      
      Serial.println("[ESP-CAM] Face Detected");        
 
      if(WiFi.status()== WL_CONNECTED){
        
        // sedning image bin over http post request
        http.begin(img_recog_url);

        // base64 encoding of the image
        String img_buffer = base64::encode((uint8_t *) fb->buf, fb->len);
        // generating json payload
        String payload = "{\"image\": \"" + img_buffer + "\", \"door\": \"" + myMACAddress + "\"}";

        // sending image to recognition server in HTTP POST request
        Serial.println("[HTTP-POST] Sending image payload...\n"); 
        Serial.println(payload);
        http.addHeader("Content-Type", "application/json");     
        int httpResponseCode = http.POST(payload);

        // if error free response recieved
        if (httpResponseCode > 0){

          Serial.print("[HTTP-POST] Response Code: ");
          Serial.println(httpResponseCode);
          
          // parsing the response data
          String response = http.getString();              // get response data
          Serial.print("[HTTP-POST] Response: ");
          Serial.println(response);
          
          int ind1 = response.indexOf(',');                // finds location of first comma
          String person = response.substring(1, ind1);     // captures person data String
          int ind2 = response.indexOf(',', ind1+1 );       // finds location of second comma
          String score = response.substring(ind1+1, ind2); // captures scorre data String

          Serial.println("[HTTP-POST] Facial Recog Results:");
          Serial.print("person = ");
          Serial.println(person);
          Serial.print("score = ");
          Serial.println(score);
          Serial.println();

          // if person data string actually has a persons name
          if (person.substring(4, 10) == "Person"){
            Serial.println("[HTTP-POST] Facial Recognision Verified!");
            // reset motion status flag
            motionDetected = false; // finished with motion detected
            faceRecogFlag  = true;  // asserting facial recog flag
            frStartTime    = millis();  // starting facial recog timeout
          }
        }
        else{
          Serial.print("[HTTP-POST] Error on sending POST: ");
          Serial.println(httpResponseCode);
        }
        // free resources
        http.end();
      }
      else{
        Serial.println("[WiFi] Disconnected");  
      }
    }
    // free image allcoation mem
    dl_matrix3du_free(image_matrix);
  }

  // motionDetection timeout    
  mdCurrTime = millis();
  if ((mdCurrTime - mdStartTime > MOTION_TIMEOUT) && motionDetected){
    Serial.println("[TIMEOUT] Motion Detection Timeout\n");
    motionDetected = false;
  }

  // nfc verification timeout    
  nfcCurrTime = millis();
  if ((nfcCurrTime - nfcStartTime > NFC_TIMEOUT) && nfcFlag){
    Serial.println("[TIMEOUT] NFC Timeout\n");
    nfcFlag = false;
  }
  
  // facial recognision timeout    
  frCurrTime = millis();
  if ((frCurrTime - frStartTime > FACE_RECOG_TIMEOUT) && faceRecogFlag){
    Serial.println("[TIMEOUT] Facial Recog Timeout\n");
    faceRecogFlag = false;
  }

  // 2FA - and operation on both verifation flags :)
  if (nfcFlag && faceRecogFlag){
//  if (faceRecogFlag){
    changeLatchState();
    nfcFlag = false;
    faceRecogFlag = false;
    Serial.println("\n[2FA] \n2FA!!! Toggling Latch\n");
  }
}
