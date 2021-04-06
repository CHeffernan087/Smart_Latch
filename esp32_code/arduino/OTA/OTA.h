#ifndef OTA_h
#define OTA_h

#include "Arduino.h"
#include <HTTPClient.h>
#include <Update.h>
#include <WiFi.h>

class OTA {
  
  public:
    OTA(String curr_version, String model, String cloudURL);
    void checkForUpdates();
    
  private:
    String model_variant;
    String cloudFunctionURL;
    String version_number;
    String getDownloadUrl();
    bool downloadUpdate(String url);
  };

#endif
