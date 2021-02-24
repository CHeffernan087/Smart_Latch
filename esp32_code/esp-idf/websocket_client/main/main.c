/* ESP32 Websocket Client
 *
 * This is a basic WebSocket client application for the ESP32 to get us up and running.
 * 
 * The application polls a push-button and will send a small request message to 'echo.websocket.org' 
 * when the button has been pressed. When the WebSocket server responds to the ESP32, the latch will
 * toggle between open and closed. The green LED will light when the door is open and the red LED
 * will light when the door is closed.
 * 
 * The button is only polled every 500ms to prevent the server from being spammed with requests.
 * So as long as the button is pressed and released within 500ms it will only send a single request.
*/

#include <stdio.h>
#include "esp_wifi.h"
#include "esp_system.h"
#include "nvs_flash.h"
#include "esp_event.h"
#include "protocol_examples_common.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"

#include "esp_log.h"
#include "esp_websocket_client.h"
#include "esp_event.h"
#include "driver/gpio.h"
#include "driver/mcpwm.h"
#include "soc/mcpwm_periph.h"

// Peripherals
#define RED_LED_OUT 26      // red led pin
#define GREEN_LED_OUT 33    // green led pin
#define WHITE_LED_OUT 14    // white led pin
#define BTN_IN 32           // input button pin
#define SERVO_PIN 18        // output PWM servo pin
#define INPUT_PIN_SEL (1ULL << BTN_IN)  // input gpio mask
#define OUTPUT_PIN_SEL ((1ULL << RED_LED_OUT) | (1ULL << GREEN_LED_OUT) | (1ULL << WHITE_LED_OUT))  // output gpio mask

// LED output macros
#define RED_ON()    gpio_set_level(RED_LED_OUT, 1);   // turn on red LED
#define RED_OFF()   gpio_set_level(RED_LED_OUT, 0);   // turn off red LED
#define GREEN_ON()  gpio_set_level(GREEN_LED_OUT, 1); // turn on green LED
#define GREEN_OFF() gpio_set_level(GREEN_LED_OUT, 0); // turn off green LED
#define WHITE_ON()  gpio_set_level(WHITE_LED_OUT, 1); // turn on white LED
#define WHITE_OFF() gpio_set_level(WHITE_LED_OUT, 0); // turn off white LED

// Tags
static const char *W_TAG = "WEBSOCKET";   // tag for websocket logs
static const char *L_TAG = "SMART-LATCH"; // tag for latch logs

// Network
static const char *MESSAGE_SEND = "ToggleLatch"; // client request message
static const char *TOGGLE_LATCH = "ToggleLatch"; // response message to toggle the latch
static const char *BOARD_ID_REQ = "boardIdReq";  // message recieved upon connection

// Timing
int           websocketStatus    = 0;       // bool value is TRUE if websocket is established
unsigned long lastUpdate         = 0;       // stores time(ms) at last latch update
unsigned long currentTime        = 0;       // stores current time(ms)
unsigned long messageInterval    = 500;     // minimum interval between messages
unsigned long connectionInterval = 10000;   // websocket connection timeout

// Latch
int latchState = 0;  // boolean latch state
int buttonValue = 0; // boolean button state

// defined websocket client in a global scope
esp_websocket_client_handle_t client;

// function to close latch
static void
close_latch()
{
    ESP_LOGI(L_TAG, "Locking the door !!!\n");
    RED_ON();
    GREEN_OFF();
    latchState = 1; // toggle latch state
    // set pwm signal to activate servo and open latch
    int duty_cycle = 500;
    mcpwm_set_duty_in_us(MCPWM_UNIT_0, MCPWM_TIMER_0, MCPWM_OPR_A, duty_cycle);
}

// function to open latch
static void open_latch()
{
    ESP_LOGI(L_TAG, "Opening the door !!!\n");
    RED_OFF();
    GREEN_ON();    
    latchState = 0; // toggle latch state
    // set pwm signal to activate servo and open latch
    int duty_cycle = 2250;
    mcpwm_set_duty_in_us(MCPWM_UNIT_0, MCPWM_TIMER_0, MCPWM_OPR_A, duty_cycle);
}

// process response message
static void process_message(char *message, int message_len)
{
    ESP_LOGI(L_TAG, "Processing Response Message ...");

    // if payload data is larger than 0 bytes
    if (message_len)
    {
        // if message requests the latch to toggle
        if (!strncmp(message, TOGGLE_LATCH, message_len))
        {
            ESP_LOGI(L_TAG, "Toggling Latch ...\n");
            // if latch state is 1
            if (latchState)
            {
                open_latch(); // open the latch
            }
            else
            {
                close_latch(); // close the latch
            }
        }
        else if (!strncmp(message, BOARD_ID_REQ, message_len))
        {
            // board should start a connection with server and give the server a way to uniquely identify the door
            char *MESSAGE_SEND = "message:boardIdRes,doorId:31415";
            char data[64];
            int len = sprintf(data, MESSAGE_SEND);
            ESP_LOGI(W_TAG, "Sending %s\n", data);
            esp_websocket_client_send_text(client, data, len, portMAX_DELAY); // send toggle request message buffer
            lastUpdate = xTaskGetTickCount() * portTICK_RATE_MS;              // update the lastUpdate value
        }
        else
        {
            ESP_LOGI(L_TAG, "Response Message '%s' Not Recognised\n", message);
        }
    }
    else
    {
        ESP_LOGI(L_TAG, "Response Message Length = %d bytes\n", message_len);
    }
}

/*

    */

// websocket event handler callback function
static void websocket_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{

    // init websocket recieved data block
    esp_websocket_event_data_t *data = (esp_websocket_event_data_t *)event_data;

    // switch case for different events
    switch (event_id)
    {

    // connection event
    case WEBSOCKET_EVENT_CONNECTED:
        ESP_LOGI(W_TAG, "WEBSOCKET_EVENT_CONNECTED");
        WHITE_ON(); // enable connection status LED
        break;

    // disconnection event
    case WEBSOCKET_EVENT_DISCONNECTED:
        ESP_LOGI(W_TAG, "WEBSOCKET_EVENT_DISCONNECTED");
        break;

    // payload response event
    case WEBSOCKET_EVENT_DATA:
        ESP_LOGI(W_TAG, "WEBSOCKET_EVENT_DATA");
        ESP_LOGI(W_TAG, "Received opcode=%d", data->op_code);

        ESP_LOGW(W_TAG, "Received=%.*s", data->data_len, (char *)data->data_ptr);
        ESP_LOGW(W_TAG, "Total payload length=%d, data_len=%d, current payload offset=%d", data->payload_len, data->data_len, data->payload_offset);
        // process and act on incoming message
        process_message((char *)data->data_ptr, data->data_len);
        break;

    // error event
    case WEBSOCKET_EVENT_ERROR:
        ESP_LOGI(W_TAG, "WEBSOCKET_EVENT_ERROR");
        break;
    }
}

// main
void app_main(void)
{

    // gpio peripherals settings
    gpio_config_t io_conf;                     // gpio config object
    io_conf.intr_type = GPIO_PIN_INTR_DISABLE; // disable interrupt
    io_conf.mode = GPIO_MODE_OUTPUT;           // set output mode
    io_conf.pin_bit_mask = OUTPUT_PIN_SEL;     // set output pins using mask
    io_conf.pull_down_en = 0;                  // disable pin pull-down
    io_conf.pull_up_en = 0;                    // disable pin pull-up
    // configure output GPIO with the given settings
    gpio_config(&io_conf);

    io_conf.pin_bit_mask = INPUT_PIN_SEL; // set output pins using mask
    io_conf.mode = GPIO_MODE_INPUT;       // set input mode
    io_conf.pull_down_en = 0;             // disable pin pull-down
    io_conf.pull_up_en = 0;               // disable pin pull-up
    // configure output GPIO with the given settings
    gpio_config(&io_conf);

    // set all outputs low
    RED_OFF();
    GREEN_OFF();
    WHITE_OFF();

    //servo mcpwm gpio initialization
    mcpwm_gpio_init(MCPWM_UNIT_0, MCPWM0A, SERVO_PIN); //Set GPIO 18 as PWM0A, to which servo is connected
    mcpwm_config_t pwm_config;
    pwm_config.frequency = 50; //frequency = 50Hz, i.e. for every servo motor time period should be 20ms
    pwm_config.cmpr_a = 0;     //duty cycle of PWMxA = 0
    pwm_config.cmpr_b = 0;     //duty cycle of PWMxb = 0
    pwm_config.counter_mode = MCPWM_UP_COUNTER;
    pwm_config.duty_mode = MCPWM_DUTY_MODE_0;
    mcpwm_init(MCPWM_UNIT_0, MCPWM_TIMER_0, &pwm_config); //Configure PWM0A & PWM0B with above settings

    // system initialisation
    ESP_LOGI(W_TAG, "[APP] Startup..");
    ESP_LOGI(W_TAG, "[APP] Free memory: %d bytes", esp_get_free_heap_size());
    ESP_LOGI(W_TAG, "[APP] IDF version: %s", esp_get_idf_version());
    esp_log_level_set("*", ESP_LOG_INFO);
    esp_log_level_set("WEBSOCKET_CLIENT", ESP_LOG_DEBUG);
    esp_log_level_set("TRANS_TCP", ESP_LOG_DEBUG);
    ESP_ERROR_CHECK(nvs_flash_init());
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    // configure websocket client
    ESP_ERROR_CHECK(example_connect());               // configures Wi-Fi
    esp_websocket_client_config_t websocket_cfg = {}; // websocket config instance
    websocket_cfg.uri = CONFIG_WEBSOCKET_URI;         // set the endpoint using the sdkconfig file
    client = esp_websocket_client_init(&websocket_cfg);
    esp_websocket_register_events(client, WEBSOCKET_EVENT_ANY, websocket_event_handler, (void *)client); // set websocket event handler

    // close the latch
    close_latch();                                       // client request message buffer
    lastUpdate = xTaskGetTickCount() * portTICK_RATE_MS; // set lastUpdate to current time

    // embedded while 1
    while (1)
    {
        buttonValue = gpio_get_level(BTN_IN);                 // get current button value
        currentTime = xTaskGetTickCount() * portTICK_RATE_MS; // set current time

        // if client is on network, button is pressed, and messageInterval is complete
        if (buttonValue && (currentTime - lastUpdate > messageInterval))
        {
            // only establishes a connection if it is already closed
            if (!websocketStatus){
                ESP_LOGI(W_TAG, "Connecting to %s...", websocket_cfg.uri);
                esp_websocket_client_start(client);
            }
            lastUpdate = xTaskGetTickCount() * portTICK_RATE_MS; // update the lastUpdate value
            websocketStatus = 1;
        }

        // if websocket connection has been established for more than specifed interval terminate it
        if (websocketStatus && (currentTime - lastUpdate >= connectionInterval))
        {
            ESP_LOGI(W_TAG, "Closing WebSocket Connection...\n");
            esp_websocket_client_stop(client);  // close websocket connection
            WHITE_OFF();                        // disable connection status LED
            websocketStatus = 0;
        }

        vTaskDelay(10 / portTICK_RATE_MS); // required scheduler delay (10ms)
    }

    // kill client
    esp_websocket_client_stop(client);
    ESP_LOGI(W_TAG, "Websocket Stopped");
    esp_websocket_client_destroy(client);
}
