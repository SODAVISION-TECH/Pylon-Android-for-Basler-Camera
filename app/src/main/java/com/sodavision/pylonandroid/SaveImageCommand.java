package com.sodavision.pylonandroid;

import android.graphics.Bitmap;

import com.basler.pylon.GrabResult;

import java.io.FileNotFoundException;
import java.io.IOException;

/** Command that converts a grabResult to a Java bitmap and saves it to storage.
 **/
public class SaveImageCommand implements ICommand{
    final String                  m_Path;
    final Bitmap.CompressFormat   m_Format;
    final GrabResult              m_GrabResult;
    final LogTarget               m_Log;
    private void Log( LogTarget.LogLevel l, String logText ) {
        if( m_Log != null) {
            m_Log.Log(l, logText);
        }
    }
    
    /** Constructor.
     *  @param path Image save path without extension.
     *  @param format Compression format. Only jpeg and png are supported.
     *  @param grabResult A valid grabResult to be converted and saved.
     *  @param log Logger implementation can be null.
     **/
    SaveImageCommand(String path, Bitmap.CompressFormat format, GrabResult grabResult, LogTarget log) {
        m_Path = path;
        m_Format = format;
        m_GrabResult = grabResult;
        m_Log = log;
    }
    @Override
    public void discard() {
        Log( LogTarget.LogLevel.Info,"Discard job: " + m_Path);
        m_GrabResult.release();
    }
    @Override
    public void execute() {
        try {
            // Convert to bitmap and save bitmap to file stream.
            MainActivity.saveImage(m_Path, m_Format, m_GrabResult.convertToBitmap(), new MainActivity());
        }
        catch(FileNotFoundException e) {
            Log( LogTarget.LogLevel.Error,"File " + m_Path + " not found");
        }
        catch( IOException e) {
            Log( LogTarget.LogLevel.Error,"File " + m_Path + " io error " + e.getMessage());
        }
        finally {
            // Release the resources. This will requeue the buffer.
            m_GrabResult.release();
        }
    }
}
