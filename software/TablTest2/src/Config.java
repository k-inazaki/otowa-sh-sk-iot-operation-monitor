//********************************************************************************
class Config {
    // アプリケーション名
    static String appName;
    // AWS-IoT
    static String clientId          = "SN000009";
    //static String subtopic          = "OTOWA/LiAlarm/Console";
    static String subtopic          = "OTOWA/LiAlarm/Thing/SN000009";
    static String pubtopicBase      = "OTOWA/LiAlarm/Thing/";
    static String pubtopic          = "";
    static String pubtopicKeepAlive = "OTOWA/LiAlarm/Thing/SN999990";
    // json-path
    static String jsonFilePath      = "/home/inazaki/OTOWA_V2/JavaExtensionPack/LiAlarmV3/req_json/";
    static String req_json          = "/home/inazaki/OTOWA_V2/JavaExtensionPack/LiAlarmV3/req_json/req.json";
    static String rsp_json          = "/home/inazaki/OTOWA_V2/JavaExtensionPack/LiAlarmV3/rsp_json/rsp.json";
    // MQTT
    static String endpoint          = "a39p3e1xsbkltp-ats.iot.ap-northeast-1.amazonaws.com";
    static String certificatePath   = "/home/inazaki/OTOWA_V2/certificate/";
    static String rootCa            = "KYOEI/AmazonRootCA1.pem";
    //static String cert              = "1c4fd32dfd97c94f7d6337aa55ece8537d58a130da9cc33e27f8deb82ce7f0a7-certificate.pem.crt";
    //static String key               = "1c4fd32dfd97c94f7d6337aa55ece8537d58a130da9cc33e27f8deb82ce7f0a7-private.pem.key";
    static String cert              = "KYOEI/a4687677187f8d0f18958f4fabfbd1ac805bb89f51bb1f3f2104b555f375ccd5-certificate.pem.crt";
    static String key               = "KYOEI/a4687677187f8d0f18958f4fabfbd1ac805bb89f51bb1f3f2104b555f375ccd5-private.pem.key";
    static int port                 = 8883;
}
//********************************************************************************
