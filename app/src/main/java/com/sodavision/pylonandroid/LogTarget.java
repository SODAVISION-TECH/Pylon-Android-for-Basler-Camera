package com.sodavision.pylonandroid;

/** Basic log target interface.
 *  Allows application and logic to share logging functionality.
 *  Implement to get text view logger or Logcat logging.
 **/
public interface LogTarget
{
    enum LogLevel
    {
        Trace,
        Info,
        Warning,
        Error
    }
    void Log(LogLevel l, String s);
}
