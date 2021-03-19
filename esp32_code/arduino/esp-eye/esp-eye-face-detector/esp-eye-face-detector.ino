#include <Arduino.h>
#include <WiFi.h>
#include <WebSocketsClient.h>

#include "esp_timer.h"
#include "esp_camera.h"
#include "camera_index.h"
#include "fd_forward.h"
#include "fr_forward.h"
#include "fr_flash.h"

const char* ssid = "McNallys";
const char* password = "mcnally123";

#define ENROLL_CONFIRM_TIMES 5
#define FACE_ID_SAVE_NUMBER 7

// Select camera model
//#define CAMERA_MODEL_WROVER_KIT
#define CAMERA_MODEL_ESP_EYE
//#define CAMERA_MODEL_M5STACK_PSRAM
//#define CAMERA_MODEL_M5STACK_WIDE
//#define CAMERA_MODEL_AI_THINKER
#include "camera_pins.h"


long current_millis;
long last_detected_millis = 0;


typedef enum
{
  START_STREAM,
  START_DETECT,
  SHOW_FACES,
  START_RECOGNITION,
  START_ENROLL,
  ENROLL_COMPLETE,
  DELETE_ALL,
} en_fsm_state;
en_fsm_state g_state;


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


// Objects
WebSocketsClient webSocket; // WebSocket object

// Timing
unsigned long messageInterval = 10000; // time interval to prevent button bounce
bool connected = false;
 
#define DEBUG_SERIAL Serial

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
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
 
    switch(type) {
        case WStype_DISCONNECTED:
            DEBUG_SERIAL.printf("[WSc] Disconnected!\n");
            connected = false;
            break;
        case WStype_CONNECTED: {
            DEBUG_SERIAL.printf("[WSc] Connected to url: %s\n", payload);
            connected = true;
 
            // send message to server when Connected
            DEBUG_SERIAL.println("[WSc] SENT: Connected");
            webSocket.sendTXT("Connected");
        }
            break;
        case WStype_TEXT:
            DEBUG_SERIAL.printf("[WSc] RESPONSE: %s\n", payload);
            DEBUG_SERIAL.printf("Response Recieved - Toggling Servo ...\n");
            break;
        case WStype_BIN:
            DEBUG_SERIAL.printf("[WSc] get binary length: %u\n", length);
            hexdump(payload, length);
            break;
        case WStype_PING:
            // pong will be send automatically
            DEBUG_SERIAL.printf("[WSc] get ping\n");
            break;
        case WStype_PONG:
            // answer to a ping we send
            DEBUG_SERIAL.printf("[WSc] get pong\n");
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
  DEBUG_SERIAL.begin(115200);
  DEBUG_SERIAL.println();

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
    DEBUG_SERIAL.printf("Camera init failed with error 0x%x", err);
    return;
  }

  sensor_t * s = esp_camera_sensor_get();
  s->set_framesize(s, FRAMESIZE_QVGA);

#if defined(CAMERA_MODEL_M5STACK_WIDE)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

  for(uint8_t t = 4; t > 0; t--) {
      DEBUG_SERIAL.printf("[SETUP] BOOT WAIT %d...\n", t);
      DEBUG_SERIAL.flush();
      delay(1000);
  }
  
  WiFi.begin(ssid, password);
  
  while ( WiFi.status() != WL_CONNECTED ) {
    delay ( 500 );
    DEBUG_SERIAL.print ( "." );
  }
  
  DEBUG_SERIAL.print("Local IP: "); 
  DEBUG_SERIAL.println(WiFi.localIP());
  
  // server address, port and URL
  // just using this websocket service for now to get a default response
  webSocket.begin("echo.websocket.org", 80, "/");
  
  // event handler
  webSocket.onEvent(webSocketEvent);
}

unsigned long lastUpdate = millis();
size_t frame_num = 0;
dl_matrix3du_t *image_matrix = NULL;
camera_fb_t *fb = NULL;



void loop() {

    webSocket.loop();

    // if connected, button is pushed, and message interval is reached we send message to server
    // try press and release button in under 500ms, otherwise it will start to spam the server
    if (connected && lastUpdate+messageInterval<millis()){
      
        int64_t start_time = esp_timer_get_time();
        /* 2. Get one image with camera */
        fb = esp_camera_fb_get();
        if (!fb)
        {
            DEBUG_SERIAL.println("Camera capture failed");
//            continue;
        }
        int64_t fb_get_time = esp_timer_get_time();
        DEBUG_SERIAL.printf("Get one frame in %lld ms.", (fb_get_time - start_time) / 1000);
        DEBUG_SERIAL.println();
        
        /* 3. Allocate image matrix to store RGB data */
        image_matrix = dl_matrix3du_alloc(1, fb->width, fb->height, 3);
        
        /* 4. Transform image to RGB */
        uint32_t res = fmt2rgb888(fb->buf, fb->len, fb->format, image_matrix->item);
        if (true != res)
        {
            DEBUG_SERIAL.printf("fmt2rgb888 failed, fb: %d", fb->len);        
            DEBUG_SERIAL.println();
            dl_matrix3du_free(image_matrix);
//            continue;
        }
        
        esp_camera_fb_return(fb);
        
        /* 5. Do face detection */
        box_array_t *net_boxes = face_detect(image_matrix, &mtmn_config);
        DEBUG_SERIAL.printf("Detection time consumption: %lldms", (esp_timer_get_time() - fb_get_time) / 1000);        
        DEBUG_SERIAL.println();
        
        if (net_boxes)
        {
            frame_num++;
            DEBUG_SERIAL.printf("DETECTED: %d\n", frame_num);        
            DEBUG_SERIAL.println();
            
            DEBUG_SERIAL.println("[WSc] SENT: Simple js client message!!");
            webSocket.sendTXT("Simple js client message!!");
            DEBUG_SERIAL.println("-> Message Sent - Waiting for Response ...");
            
            lastUpdate = millis();
//            dl_lib_free(net_boxes->score);
//            dl_lib_free(net_boxes->box);
//            dl_lib_free(net_boxes->landmark);
//            dl_lib_free(net_boxes);
        }
        
        dl_matrix3du_free(image_matrix);
        
    }
    
}
