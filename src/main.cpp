//********************************************************************************
/*
GitHub
*/
//********************************************************************************
#include <M5CoreS3.h>
#include <SD.h>
#include <vector>
//********************************************************************************
enum class EventType : uint8_t
{
    NONE     = 0,
    ON       = 1,
    OFF      = 2,
    PROGRESS = 3
};

struct LogData
{
    EventType type;
    uint32_t total;
    uint32_t on;
    uint32_t off;
};
//********************************************************************************
std::vector<LogData> bufferA;
std::vector<LogData> bufferB;
std::vector<LogData>* pWrite = &bufferA;
std::vector<LogData>* pRead  = nullptr;

volatile bool dataReady         = false;
const int BATCH_SIZE            = 10;
const uint32_t FLUSH_TIMEOUT_MS = 30000;  // 30秒間イベントなしで強制保存

// 稼働状態
uint32_t upTimeCount = 0;
uint32_t onCount = 0, offCount = 0;
int pinsts_old = HIGH;
int input1;
//********************************************************************************
const char* getEventName(EventType type)
{
    switch (type)
    {
        case EventType::ON:
            return "on!";
        case EventType::OFF:
            return "off!";
        case EventType::PROGRESS:
            return "progress!";
        default:
            return "unknown";
    }
}

// バッファの切り替え（フリップ）処理：タスク内から呼び出し
void flipBuffer()
{
    pRead  = pWrite;
    pWrite = (pWrite == &bufferA) ? &bufferB : &bufferA;
    pWrite->clear();

    __asm__ __volatile__("memw");
    dataReady = true;
}
//********************************************************************************
void LoggerTask(void* pvParameters)
{
    const TickType_t xFrequency = pdMS_TO_TICKS(1000);
    TickType_t xLastWakeTime    = xTaskGetTickCount();
    uint32_t lastEventMillis    = millis();

    bufferA.reserve(BATCH_SIZE);
    bufferB.reserve(BATCH_SIZE);

    for (;;)
    {
        int pinsts_new     = digitalRead(input1);
        uint32_t now       = millis();
        bool eventOccurred = false;

        // 1. カウントおよびエッジ判定
        // --- 稼働時間カウント(10秒毎にPROGRESS) ---
        if (pinsts_new == LOW)
        {
            upTimeCount++;
            if (upTimeCount % 10 == 0)
            {
                Serial.println(getEventName(EventType::PROGRESS));
                pWrite->push_back({EventType::PROGRESS, upTimeCount, onCount, offCount});
                eventOccurred = true;
            }
        }

        // --- ON/OFFエッジ検出 ---
        if (pinsts_new != pinsts_old)
        {
            EventType et = (pinsts_new == LOW) ? EventType::ON : EventType::OFF;
            if (et == EventType::ON)
            {
                Serial.println(getEventName(EventType::ON));
                onCount++;
            }
            else
            {
                Serial.println(getEventName(EventType::OFF));
                offCount++;
            }
            pWrite->push_back({et, upTimeCount, onCount, offCount});
            eventOccurred = true;
            pinsts_old    = pinsts_new;
        }

        // 2. タイムアウトおよびバッファ満杯の判定
        if (eventOccurred)
            lastEventMillis = now;

        if (!pWrite->empty())
        {
            // 判定：10件溜まった、または30秒経過した
            if (pWrite->size() >= BATCH_SIZE || (now - lastEventMillis >= FLUSH_TIMEOUT_MS))
            {
                flipBuffer();
                lastEventMillis = now;  // フラッシュ後はタイマーリセット
            }
        }

        vTaskDelayUntil(&xLastWakeTime, xFrequency);
    }
}
//********************************************************************************
void setup()
{
    auto cfg = M5.config();
    CoreS3.begin(cfg);
    Serial.begin(115200);
    delay(3000);
    
    if (!SD.begin(4))
        Serial.println("SD Error");

    input1 = CoreS3.Ex_I2C.getSDA();
    pinMode(input1, INPUT_PULLUP);

    xTaskCreatePinnedToCore(LoggerTask, "Logger", 4096, NULL, 1, NULL, 1);
    Serial.println("System Start! (Buffer: 10 or Timeout: 30s)");
}
//********************************************************************************
void loop()
{
    CoreS3.update();

    if (dataReady)
    {
        // 読み出しポインタをコピーして即座にフラグを下ろす
        std::vector<LogData>* targetBuffer = pRead;
        dataReady                          = false;

        File f = SD.open("/uptime_log.csv", FILE_APPEND);
        if (f)
        {
            Serial.println(">>> Batch processing...");
            for (const auto& log : *targetBuffer)
            {
                const char* name = getEventName(log.type);
                f.printf("%s, %d, %d, %d\r\n", name, log.total, log.on, log.off);
                Serial.printf("%s [%04d, %04d, %04d]\r\n", name, log.total, log.on, log.off);
            }
            f.close();
        }
    }

    // ボタンA（画面左側タッチ）でリセット
    if (M5.BtnA.pressedFor(3000))
    {
        upTimeCount = 0;
        onCount     = 0;
        offCount    = 0;
        Serial.println("!!! Reset Counters !!!");
        delay(500);
    }
    delay(1);
}