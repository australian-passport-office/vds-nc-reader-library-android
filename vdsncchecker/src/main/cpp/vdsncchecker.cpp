#include <iostream>
#include <jni.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <pthread.h>
#include <jni.h>
#include <string>
#include <cstdlib>
#include <android/log.h>


#define  LOG_TAG    "testjni"
#define  ALOG(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)


// Anti-debugging #############################################################
// See: https://mobile-security.gitbook.io/mobile-security-testing-guide/android-testing-guide/0x05j-testing-resiliency-against-reverse-engineering

static int child_pid;
static pthread_t monitor_thread;

struct thread_data {
    int  thread_id;
    bool log_out;
};

static struct thread_data td[1];

sigset_t mask;

void kill_anti_debugs(){
    if(child_pid > 0){
        kill(child_pid, SIGKILL);
        child_pid = 0;
    }
    if(monitor_thread != NULL){
        pthread_kill(monitor_thread, SIGUSR1);
        monitor_thread = NULL;
    }
}


void *monitor_pid(void *ta) {
    int status;

    struct thread_data *data;
    data = (struct thread_data *) ta;

    if(data->log_out){
        ALOG("Thread %d", data->thread_id);
    }

    /* Child status should never change. */
    // wait for kill signal

    sigwait(&mask, &status);
    if(data->log_out){
        ALOG("Thread %d killed!", data->thread_id);
    }
    pthread_exit(NULL);
}
void anti_debug(bool log_out) {

    //setting up the signal mask
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    pthread_sigmask(SIG_BLOCK, &mask, NULL); //sets up the signal mask for this thread

    child_pid = fork();
    if (child_pid == 0)
    {
        int ppid = getppid();
        int status;
        if (ptrace(PTRACE_ATTACH, ppid, NULL, NULL) == 0)
        {
            waitpid(ppid, &status, 0);
            ptrace(PTRACE_CONT, ppid, NULL, NULL);
            while (waitpid(ppid, &status, 0)) {
                if (WIFSTOPPED(status)) {
                    ptrace(PTRACE_CONT, ppid, NULL, NULL);
                } else {
                    // Process has exited
                    //_exit(0);
                    kill_anti_debugs();

                }
            }
        }
    } else {
        if(log_out){
            ALOG("CHILD PID: %d", child_pid);
        }
        /* Start the monitoring thread */
        td[0].thread_id = child_pid;
        td[0].log_out = log_out;

        int tid = pthread_create(&monitor_thread, NULL, monitor_pid, (void *)&td[0]);
        if(tid){
            if(log_out) {
                ALOG("Could not create thread");
            }
        }


    }
}

extern "C"
JNIEXPORT void JNICALL
Java_au_gov_dfat_lib_vdsncchecker_security_SecurityManager_ad(JNIEnv *env, jobject thiz, jboolean log_out) {
    anti_debug(log_out);
}

extern "C"
JNIEXPORT void JNICALL
Java_au_gov_dfat_lib_vdsncchecker_security_SecurityManager_killanti(JNIEnv *env, jobject thiz) {
    kill_anti_debugs();
}

// /Anti-debugging #############################################################

// Root detection ##############################################################

/**
 *
 * function that checks for the su binary files and operates even if
 * root cloak is installed
 * @return integer 1: device is rooted, 0: device is not
 *rooted
*/
extern "C"
JNIEXPORT int JNICALL
Java_au_gov_dfat_lib_vdsncchecker_security_SecurityManager_b(JNIEnv *env, jobject thiz) {
    const char *paths[] ={"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                          "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                          "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};

    int counter = 0;
    while (counter < 9) {
        if(FILE *file = fopen(paths[counter],"r")) {
            fclose(file);
            return 1;
        }
        counter++;
    }
    return 0;
}

