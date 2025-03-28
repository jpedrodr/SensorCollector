SensorCollector

SensorCollector is an Android application that collects accelerometer data at configurable frequencies (100Hz, 200Hz, or MAX) and generates periodic reports using a native C++ library. The collected data is stored in CSV files and analyzed to detect gaps in sensor readings.

Features:
1 - Collect accelerometer data at different frequencies.
2 - Store data in structured CSV format.
3 - Generate reports every 15 minutes.
4 - Detect and log gaps in sensor readings. Uses a C++ library (sensor_analyzer.so) for efficient data processing.

App Structure:
1 - MainActivity.kt: Handles UI interactions.
2 - MainViewModel.kt: Manages the app logic and interacts with the native library.
3 - SensorManager.kt: Handles sensor data collection logic.
3 - FileManager.kt: Handles file data writing/reading logic.
4 - SensorAnalyzer.kt: Interface to communicate with the C++ library.
5 - SensorAnalyzer.cpp: C++ library for report generation.

UI:
1 - Select frequency - allows the selection of the frequency.
2 - Start collecting - start collecting sensor data and writes it the session file. If the file doesn't exist, creates it and add headers before writing data. Needs a session name before collection can start.
3 - Stop collecting - stops collecting sensor data.
4 - View session data - shows the data file of the given session data as a grid inside the app. Displays an error if no session name has been provided or if no session file exists.
5 - View last report - shows the last report file of the given session data as a grid inside the app. Needs a session name to display the report data. Displays an error if no session name has been provided or if no report file exists. NOTE: you might need to scroll down if

Report Format:
The generated report contains the following columns:
    report_timestamp: Timestamp when the report was generated.
    collection_start_time: Timestamp of the first collected sample.
    total_samples: Number of collected data points.
    total_gaps_duration: Total duration of detected gaps in seconds.

Configuration:
The expected sensor frequencies are:
    100Hz -> 10,000 microseconds delay per reading
    200Hz -> 5,000 microseconds delay per reading
    MAX -> Fastest possible sampling rate

License:
This project is licensed under the MIT License.


https://github.com/jpedrodr/SensorCollector
