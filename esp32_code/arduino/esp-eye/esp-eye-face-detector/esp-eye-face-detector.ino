#include <Arduino.h>
#include <esp_now.h>
#include <WiFi.h>
#include <WebSocketsClient.h>

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

#define MCNALLY

#if defined(MCNALLY)
const char* ssid = "McNallys";
const char* password = "mcnally123";
#endif

#if defined(DONEGAN)
const char* ssid = "";
const char* password = "";
#endif

// RECEIVER MAC Address
// All F's sends to all boards on network
uint8_t broadcastAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

// Specific to McNally's Boards
//uint8_t broadcastAddress[] = {0xAC, 0x67, 0xB2, 0x67, 0xA1, 0x9C};
//uint8_t broadcastAddress[] = {0x24, 0x0A, 0xC4, 0xE7, 0xF7, 0x94};

// websocket
WebSocketsClient webSocket; // websocket object
bool connected = false;     // websocket connection status

// motion detection
bool motionDetected = false;  // motion detection status

// config face detection model parameters
static inline mtmn_config_t app_mtmn_config()
{
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

// Structure of send data
typedef struct struct_message {
  char a[32];
  int b;
  float c;
  String d;
  bool e;
} struct_message;

// Create a struct_message called myData
struct_message myData;

// callback function that will be executed when data is received
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
  memcpy(&myData, incomingData, sizeof(myData));
  Serial.print("Bytes received: ");
  Serial.println(len);
  Serial.print("Char: ");
  Serial.println(myData.a);
  Serial.print("Int: ");
  Serial.println(myData.b);
  Serial.print("Float: ");
  Serial.println(myData.c);
  Serial.print("String: ");
  Serial.println(myData.d);
  Serial.print("Bool: ");
  Serial.println(myData.e);
  Serial.println();

  // if string is motion and bool is true
  if(myData.e && (myData.d == "motion")){
    motionDetected = true;
  }
}


// NOTE: likely to be depricated in the future
// if binary is recieved we print dump in hex form
void hexdump(const void *mem, uint32_t len, uint8_t cols = 16)
{
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
        case WStype_CONNECTED: {
            Serial.printf("[WSc] Connected to url: %s\n", payload);
            connected = true;
 
            // send message to server when Connected
            Serial.println("[WSc] SENT: Connected");
        }
            break;
        case WStype_TEXT:            
            Serial.printf("\nResponse Recieved - Toggling Servo ...\n");            
            Serial.printf("[WSc] RESPONSE: %s\n", payload);
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
        config.jpeg_quality = 10;
        config.fb_count = 2;
    } else {
        config.frame_size = FRAMESIZE_SVGA;
        config.jpeg_quality = 12;
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
      Serial.println("Setting as a Wi-Fi Station..");
    }
    Serial.print("Station IP Address: ");
    Serial.println(WiFi.localIP());
    Serial.print("Wi-Fi Channel: ");
    Serial.println(WiFi.channel());
  
    // Init ESP-NOW
    if (esp_now_init() != ESP_OK) {
      Serial.println("Error initializing ESP-NOW");
      return;
    }
    
    // Once ESPNow is successfully Init, we will register for recv CB to
    // get recv packer info
    esp_now_register_recv_cb(OnDataRecv);
  
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
    webSocket.begin("echo.websocket.org", 80, "/");
    
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

    // if connected and message interval is reached we send message to server
    // will be adding in comms from the esp32 board to init this
    if (connected && motionDetected){

        // get current time
        int64_t start_time = esp_timer_get_time();
        
        // get one image with camera
        fb = esp_camera_fb_get();
        // if image was not retrieved
        if (!fb){
            Serial.println("Camera capture failed");
            return;
        }

        // get time again
        int64_t fb_get_time = esp_timer_get_time();
        Serial.printf("Get one frame in %lld ms.\n", (fb_get_time - start_time) / 1000);
        
        // allocate image matrix to store RGB data
        image_matrix = dl_matrix3du_alloc(1, fb->width, fb->height, 3);
        
        // transform image to RGB
        uint32_t res = fmt2rgb888(fb->buf, fb->len, fb->format, image_matrix->item);
        // could not transfrom to RGB
        if (true != res){
            Serial.printf("fmt2rgb888 failed, fb: %d\n", fb->len);
            dl_matrix3du_free(image_matrix);
            return;
        }

        // halt camera
        esp_camera_fb_return(fb);
        
        // do face detection
        box_array_t *net_boxes = face_detect(image_matrix, &mtmn_config);
        // print detection time in ms
        Serial.printf("Detection time consumption: %lldms\n", (esp_timer_get_time() - fb_get_time) / 1000);        

        // if face was detected, boxes would be generated
        if (net_boxes){

            // print how many faces were detected
            Serial.printf("\nFACE DETECTED\n");        

            // sending RGB image bin
            Serial.println("[WSc] SENDING - Detected Face Image");
            webSocket.sendBIN(fb->buf, fb->len);
            Serial.println("-> Message Sent - Waiting for Response ...");

            // reset motion status flag
            motionDetected = false;
        }

        // free image allcoation mem
        dl_matrix3du_free(image_matrix);
    }    
}
