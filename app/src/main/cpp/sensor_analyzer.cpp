#include <jni.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_jpdr_sensorcollector_MainViewModel_createReport(
        JNIEnv *env,
        jobject thiz,
        jstring session_name
) {
    const char *sessionName = env->GetStringUTFChars(session_name, 0);

    __android_log_print(ANDROID_LOG_INFO, "joaorosa", "createReport called %s", sessionName);
}