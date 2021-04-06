#include "Arduino.h"
#include "OTA.h"

/* 
 * Initialise OTA class with version number model (ESP32/ESP-EYE) and cloud URL
 */
OTA::OTA(String curr_version, String model, String cloudURL)
{
  cloudFunctionURL = cloudURL;
  model_variant = model;
  version_number = curr_version;
}


/* 
 * Check if needs to update the device and returns the download url.
 */
String OTA::getDownloadUrl()
{
  HTTPClient http;
  String downloadUrl;
  Serial.print("[HTTP] Begin Check for OTA updates.\n");

  String url = cloudFunctionURL;
  url += String("?version=") + version_number;
  url += String("&device=") + model_variant;
  Serial.println(url);
  http.begin(url);

  Serial.print("[HTTP] GET...\n");
  // start connection and send HTTP header
  int httpCode = http.GET();

  // httpCode will be negative on error
  if (httpCode > 0)
  {
    // HTTP header has been send and Server response header has been handled
    Serial.printf("[HTTP] GET... code: %d\n", httpCode);

    // file found at server
    if (httpCode == HTTP_CODE_OK)
    {
      String payload = http.getString();
      Serial.println(payload);
      downloadUrl = payload;
    } else {
      Serial.println("Device is up to date!");
    }
  }
  else
  {
    Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
  }

  http.end();

  return downloadUrl;
}


/* 
 * Download binary image and use Update library to update the device.
 */
bool OTA::downloadUpdate(String url)
{
  WiFiClient client;
  HTTPClient http;
  Serial.print("[HTTP] Download begin...\n");

  http.begin(url);

  Serial.print("[HTTP] GET...\n");
  // start connection and send HTTP header
  int httpCode = http.GET();
  if (httpCode > 0)
  {
    // HTTP header has been send and Server response header has been handled
    Serial.printf("[HTTP] GET... code: %d\n", httpCode);

    // file found at server
    if (httpCode == HTTP_CODE_OK)
    {

      int contentLength = http.getSize();
      Serial.println("contentLength : " + String(contentLength));

      if (contentLength > 0)
      {
        bool canBegin = Update.begin(contentLength);
        if (canBegin)
        {
          WiFiClient stream = http.getStream();
          Serial.println("Begin OTA. This may take 2 - 5 mins to complete. Things might be quite for a while.. Patience!");
          size_t written = Update.writeStream(stream);

          if (written == contentLength)
          {
            Serial.println("Written : " + String(written) + " successfully");
          }
          else
          {
            Serial.println("Written only : " + String(written) + "/" + String(contentLength) + ". Retry?");
          }

          if (Update.end())
          {
            Serial.println("OTA done!");
            if (Update.isFinished())
            {
              Serial.println("Update successfully completed. Rebooting.");
              ESP.restart();
              return true;
            }
            else
            {
              Serial.println("Update not finished? Something went wrong!");
              return false;
            }
          }
          else
          {
            Serial.println("Error Occurred. Error #: " + String(Update.getError()));
            return false;
          }
        }
        else
        {
          Serial.println("Not enough space to begin OTA");
          client.flush();
          return false;
        }
      }
      else
      {
        Serial.println("There was no content in the response");
        client.flush();
        return false;
      }
    }
    else
    {
      return false;
    }
  }
  else
  {
    return false;
  }
}


/* 
 * Check if a new version is available and download if so.
 */
void OTA::checkForUpdates()
{
  String downloadUrl = getDownloadUrl();
  if (downloadUrl.length() > 0)
  {
    bool success = downloadUpdate(downloadUrl);
    if (!success)
    {
      Serial.println("Error updating device");
    }
  }
}
