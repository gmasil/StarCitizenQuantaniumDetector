#include <LedControl.h>

#define DIN 15
#define CLK 16
#define CS 14

LedControl lc = LedControl(DIN, CLK, CS, 1); // DIN CLK CS

// display
#define MAXBLINK 20
int timeSeconds = 0;
bool displayOn = false;
int blinkCount = MAXBLINK;


void setup() {
  lc.shutdown(0, false);
  lc.setIntensity(0, 7);
  lc.clearDisplay(0);
  Serial.begin(9600);
}

void loop() {
  // serial
  if (Serial.available() > 0) {
    String data = Serial.readStringUntil(';');
    if (data.startsWith("time:")) {
      setNewTime(data.substring(5).toInt() - 1);
    } else if (data.equals("handshake")) {
      Serial.write("handshake");
      lc.setChar(0, 6, 'h', false);
      lc.setChar(0, 5, 'a', false);
      lc.setChar(0, 4, 'n', false);
      lc.setChar(0, 3, 'd', false);
      lc.setChar(0, 2, '5', false);
      lc.setChar(0, 1, 'h', false);
      delay(1000);
      lc.clearDisplay(0);
    }
  }
  // display
  if (timeSeconds > 0) {
    printTime(timeSeconds);
    timeSeconds--;
    delay(1000);
  } else {
    // blink
    if (blinkCount < MAXBLINK) {
      if (displayOn) {
        displayOn = false;
        lc.clearDisplay(0);
        blinkCount++;
      } else {
        displayOn = true;
        printTime(timeSeconds);
      }
      delay(100);
    }
  }
}

void setNewTime(int timeS) {
  timeSeconds = timeS;
  displayOn = true;
  blinkCount = 0;
}

void printTime(int timeS) {
  if (timeS >= 0) {
    int index = 6;
    int hours = timeS / 60 / 60;
    if (hours > 99) {
      hours = 99;
    }
    lc.setDigit(0, index--, hours / 10, false);
    lc.setDigit(0, index--, hours % 10, true);
    int minutes = timeS / 60 % 60;
    lc.setDigit(0, index--, minutes / 10, false);
    lc.setDigit(0, index--, minutes % 10, true);
    int seconds = timeS % 60;
    lc.setDigit(0, index--, seconds / 10, false);
    lc.setDigit(0, index--, seconds % 10, false);
  }
}
