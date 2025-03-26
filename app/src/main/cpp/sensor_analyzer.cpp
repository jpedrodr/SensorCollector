#include <jni.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "joaorosa"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

const std::string BASE_DIRECTORY = "/storage/emulated/0/Android/data/com.jpdr.sensorcollector/files/SensorCollector/collects/";
const std::string REPORT_DIRECTORY = "/report/";
const std::string REPORT_FILE_PREFIX = "accelerometer_report_";
const std::string REPORT_FILE_EXTENSION = ".csv";
const std::string HEADER_TIMESTAMP = "report_timestamp";
const std::string HEADER_START_TIME = "collection_start_time";
const std::string HEADER_TOTAL_SAMPLES = "total_samples";
const std::string HEADER_TOTAL_GAPS_DURATION = "total_gaps_duration";

std::string getNextReportFileName(const std::string &sessionName) {
    // Define the base directory and file name components
    std::string reportDirectory = BASE_DIRECTORY + sessionName + REPORT_DIRECTORY;

    // Start with the first report number
    int reportNumber = 1;
    std::string reportFilePath;

    // Loop to find the next available report file name
    while (true) {
        std::stringstream fileNameStream;
        fileNameStream << REPORT_FILE_PREFIX << reportNumber << REPORT_FILE_EXTENSION;
        reportFilePath = reportDirectory + fileNameStream.str();

        // Check if the file already exists
        if (!std::filesystem::exists(reportFilePath)) {
            return reportFilePath;  // Return the available file name
        }

        reportNumber++;  // Increment the number to check the next report number
    }
}

std::string formatTime(long long timestamp) {
    // Convert long long timestamp to time_t (seconds since epoch)
    auto time = static_cast<std::time_t>(timestamp);

    // Convert to struct tm for local time
    std::tm* timeInfo = std::localtime(&time);

    // Format the time into a string with the required format
    std::ostringstream oss;
    oss << std::put_time(timeInfo, "%Y-%m-%dT%H:%M:%S");

    return oss.str();
}

std::string getCurrentTimeFormatted() {
    auto now = std::chrono::system_clock::now();
    std::time_t now_time_t = std::chrono::system_clock::to_time_t(now);
    std::tm tm = *std::localtime(&now_time_t);

    std::ostringstream oss;
    oss << std::put_time(&tm, "%Y-%m-%dT%H:%M:%S");
    return oss.str();
}

std::string getFirstTimestampFromDataFile(const std::string &dataFilePath) {
    std::ifstream file(dataFilePath);

    // check if the file is open
    if (!file.is_open()) {
        return "";  // return an invalid value if the file can't be opened
    }

    std::string line;

    LOGD("HEREEEEE");

    // skip the first line (header)
    if (std::getline(file, line)) {
        LOGD("Skipped header line:  %s", line.c_str());
        // read the second line (data)
        if (std::getline(file, line)) {
            LOGD("Second line:  %s", line.c_str());
            std::stringstream ss(line);
            std::string timestamp_str;

            // read the first value (timestamp) in the second line
            std::getline(ss, timestamp_str, ',');  // Read until the first comma

            LOGD("1st timestamp: %s", timestamp_str.c_str());

            // convert the timestamp to a long long (nanoseconds)
            try {
                // Convert string to long long and format it
                return formatTime(std::stoll(timestamp_str));
            } catch (const std::invalid_argument& e) {
                std::cerr << "Invalid timestamp: " << e.what() << std::endl;
                return "";  // Return an invalid value in case of error
            }
        }
    }

    return "";  // Return an invalid value if file is empty or no data found
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jpdr_sensorcollector_view_MainViewModel_createReport(
        JNIEnv *env,
        jobject thiz,
        jstring session_name
) {
    const char *sessionNameCStr = env->GetStringUTFChars(session_name, nullptr);
    std::string sessionName(sessionNameCStr);

    std::string dataFilePath = BASE_DIRECTORY + sessionName + "/data/accelerometer.csv";

    // Call the method to get the next available report file name
    std::string reportFilePath = getNextReportFileName(sessionName);

    std::ifstream dataFile(dataFilePath);

    if (!dataFile) {
        LOGD("Failed to open file: %s", dataFilePath.c_str());
        return;
    }

    int totalSamples = 0;
    std::string line;
    while (std::getline(dataFile, line)) {
        totalSamples++;
    }
    dataFile.close();

    std::ofstream reportFile(reportFilePath);
    if (!reportFile) {
        std::cerr << "Failed to create output file: " << reportFilePath << std::endl;
        return;
    }

    // headers
    reportFile << HEADER_TIMESTAMP << ","
               << HEADER_START_TIME << ","
               << HEADER_TOTAL_SAMPLES << ","
               << HEADER_TOTAL_GAPS_DURATION << std::endl;

    // actual data
    reportFile << getCurrentTimeFormatted() << ","
               << getFirstTimestampFromDataFile(dataFilePath) << ","
               << totalSamples << ","
               << HEADER_TOTAL_GAPS_DURATION << std::endl;

    reportFile.close();

    LOGD("Timetamp: %s", getCurrentTimeFormatted().c_str());
    LOGD("Total lines in file: %d", totalSamples);
}
