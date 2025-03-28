#include <jni.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "sensor_analyzer"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

const std::string BASE_DIRECTORY = "/storage/emulated/0/Android/data/com.jpdr.sensorcollector/files/SensorCollector/collects/";
const std::string REPORT_DIRECTORY = "/report/";
const std::string REPORT_FILE_PREFIX = "accelerometer_report_";
const std::string REPORT_FILE_EXTENSION = ".csv";
const std::string HEADER_TIMESTAMP = "report_timestamp";
const std::string HEADER_START_TIME = "collection_start_time";
const std::string HEADER_TOTAL_SAMPLES = "total_samples";
const std::string HEADER_TOTAL_GAPS_DURATION = "total_gaps_duration";

/**
 * Gets the path for the next report file based on the previous files.
 * It counts how many report files exist and gets the next one based on it.
 * For example, if there are 3 report files, next report file will be report_4
 * @param sessionName the session of the reports
 * @return the path for the next report file
 */
std::string getNextReportFilePath(const std::string &sessionName) {
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

/**
 * Formats from nanoseconds to ISO 8601
 * @param timestamp the timestamp in nanoseconds
 * @return a string of the formatted time
 */
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

/**
 * Gets the current time formatted
 * @return a string of the current time as ISO 8601
 */
std::string getCurrentTimeFormatted() {
    auto now = std::chrono::system_clock::now();
    std::time_t now_time_t = std::chrono::system_clock::to_time_t(now);
    std::tm tm = *std::localtime(&now_time_t);

    std::ostringstream oss;
    oss << std::put_time(&tm, "%Y-%m-%dT%H:%M:%S");
    return oss.str();
}

/**
 * Gets the first timestamp of the data file as ISO 8601
 * @param dataFilePath
 * @return the first timestamp of the data file as ISO 8601
 */
std::string getFirstTimestampFromDataFile(const std::string &dataFilePath) {
    std::ifstream file(dataFilePath);

    // check if the file is open
    if (!file.is_open()) {
        return "";  // return an invalid value if the file can't be opened
    }

    std::string line;

    // skip the first line (header)
    if (std::getline(file, line)) {
        // read the second line (data)
        if (std::getline(file, line)) {
            std::stringstream ss(line);
            std::string timestamp_str;

            // read the first value (timestamp) in the second line
            std::getline(ss, timestamp_str, ',');  // Read until the first comma

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
Java_com_jpdr_sensorcollector_SensorAnalyzer_createReport(
        JNIEnv *env,
        jobject thiz,
        jstring session_name,
        jint delay_microseconds
) {
    const char *sessionNameCStr = env->GetStringUTFChars(session_name, nullptr);
    std::string sessionName(sessionNameCStr);

    std::string dataFilePath = BASE_DIRECTORY + sessionName + "/data/accelerometer.csv";

    // Call the method to get the next available report file name
    std::string reportFilePath = getNextReportFilePath(sessionName);

    std::ifstream dataFile(dataFilePath);

    if (!dataFile) {
        LOGD("Failed to open file: %s", dataFilePath.c_str());
        return;
    }

    int totalSamples = 0;
    long long totalGapDurationInMicroseconds = 0;  // Total gap duration in microseconds
    std::string line;

    // Read and discard the header line
    if (!std::getline(dataFile, line)) {
        LOGD("Empty or invalid file: %s", dataFilePath.c_str());
        return;
    }

    long long prevTimestamp = -1;

    while (std::getline(dataFile, line)) {
        totalSamples++;
        // get the first column (timestamp)
        std::stringstream ss(line);

        std::string timestampStr;
        if (!std::getline(ss, timestampStr, ',')) continue;

        // the file timestamp is in nanoseconds
        long long currentTimestampInNanoseconds = std::stoll(timestampStr);
        // convert nanoseconds to microseconds
        long long currentTimestampInMicroseconds = currentTimestampInNanoseconds / 1000;

        if (prevTimestamp != -1) {
            long long expectedTimestamp = prevTimestamp + delay_microseconds;
            if (currentTimestampInMicroseconds > expectedTimestamp) {
                // here means there was a gap, the actual reading was later (>) than the expected
                // so get the different and add it to the total gaps
                totalGapDurationInMicroseconds += (currentTimestampInMicroseconds - expectedTimestamp);
            }
        }
        prevTimestamp = currentTimestampInMicroseconds;
    }
    dataFile.close();

    // Convert gap duration from microseconds to seconds
    // static_cast<double> to prevent narrowing conversion
    double totalGapDurationInSeconds = static_cast<double>(totalGapDurationInMicroseconds) / 1'000'000.0;
    double totalGapDurationInSecondsRounded = std::round(totalGapDurationInSeconds * 10) / 10;

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
               << totalGapDurationInSecondsRounded << std::endl;

    reportFile.close();

    LOGD("Timestamp: %s", getCurrentTimeFormatted().c_str());
    LOGD("Total lines in file: %d", totalSamples);
    LOGD("Total gap duration (s): %.1f", totalGapDurationInSecondsRounded);
    LOGD("Report file path: %s", reportFilePath.c_str());
    LOGD("--------------------");
}
