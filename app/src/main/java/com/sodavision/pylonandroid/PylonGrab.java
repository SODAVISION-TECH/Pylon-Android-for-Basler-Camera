package com.sodavision.pylonandroid;

import android.graphics.Bitmap;

import com.basler.pylon.EGrabStrategy;
import com.basler.pylon.ETimeoutHandling;
import com.basler.pylon.GrabResult;
import com.basler.pylon.InstantCamera;
import com.basler.pylon.TlFactory;
import com.basler.pylon.pylon;
import com.sodavision.pylonandroid.LogTarget.LogLevel;

import org.genicam.genapi.IInteger;
import org.genicam.genapi.genapi;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Wrapper class around pylon functions
 **/
public class PylonGrab implements AutoCloseable{


    /** Grab image with pylon and convert the result to Java bitmap.
     **/
    public InstantCamera getCameraInstance() {
        return m_Camera;
    }

    /** Grab image with pylon and convert the result to Java bitmap.
     **/
    public Bitmap grabImage(){

        Bitmap result = null;
        try {
            // The grabResult will contain the image data and result information.
            GrabResult grabResult = new GrabResult();

            /** Wait for one image.
             * Returns true if the call successfully retrieved a grab result and the grab succeeded.
             * the phone can not deal with too many images, it may cause the lost frames, so can't use m_Camera.grabOne
             * if you wait to grab image continuously
             */
            //boolean wasGrabSucceeded = m_Camera.grabOne(5000, grabResult, ETimeoutHandling.TimeoutHandling_Return);
            boolean wasGrabSucceeded = m_Camera.retrieveResult(5000, grabResult, ETimeoutHandling.TimeoutHandling_Return);

            if( wasGrabSucceeded )
            {
//                Log(LogLevel.Info, "Width: " + grabResult.getWidth() + " Height: " + grabResult.getHeight()
//                        + " Pixel format: " + grabResult.getPixelType().toString() + " Payload size: " + grabResult.getPayloadSize());

                // Call the convertToBitmap function to convert the camera image
                // from transfer format to ARGB8 display bitmap.
                result = grabResult.convertToBitmap();
            }
            else
                Log(LogLevel.Error, "Grab failed: " + grabResult.getErrorDescription());
        }
        catch(Exception e){
            Log(LogLevel.Info, "Exception in getImage(): " + e.getMessage() );
        }
        return result;
    }
    
    /** Grabs 40 images and saves them to folder.
     *  @param path Path to save the images to.
     *  @param format Bitmap compression format to be used. Quality is fixed at 100 %.
     **/
    public Bitmap grabImageSeq(String path, Bitmap.CompressFormat format) {
        CommandProcessor processor = null;
        Bitmap result = null;
        try {
            // Start the command processing thread.
            // Converting Bayer image formats and compressing images to jpeg or png
            // should not be done in the acquisition thread.
            // The processing queue is set to max capacity of 3 images.
            // There are 10 buffers queued with the stream engine by default.
            // This will ensure that there are always enough buffers available.
            // If the system can not save images fast enough, images are skipped.
            processor = new CommandProcessor(3);
            processor.start();
            long count = 0;

            // Start the acquisition logic in pylon.
            // The grab engine will only keep the latest buffer and if the user is not fast enough,
            // older buffers are requeued. This ensures that as long as no buffers are hold by the user,
            // the stream engine will not run out of buffers.
            m_Camera.startGrabbing(EGrabStrategy.GrabStrategy_LatestImageOnly);
            while (count < 40 )
            {
                // We create a new GrabResult for each image.
                // It will be passed to the processing thread
                // where it will be debayered and compressed.
                // This will be passed to the save image command.
                // Make sure to call grabResult.release() to requeue the buffers.
                GrabResult grabResult = new GrabResult();

                // Wait for a grabResult with 5000 ms timeout.
                boolean wasRetrieveResultSucceeded = m_Camera.retrieveResult(5000, grabResult);

                // It needs to be checked whether the grab represented by the grab result has been successful
                if (wasRetrieveResultSucceeded && grabResult.grabSucceeded())
                {
                    Log(LogLevel.Info, "Image Nr: " +count +" Width: " + grabResult.getWidth() + " Height: " + grabResult.getHeight()
                            + " Pixel format: " + grabResult.getPixelType().toString() + " Payload size: " + grabResult.getPayloadSize());

                    // For visualisation only we save the last image.
                    if(count >= 0)
                        result = grabResult.convertToBitmap();

                    // Parcel the grabResult and compression parameter for an SaveImageCommand and enqueue it.
                    processor.append( new com.sodavision.pylonandroid.SaveImageCommand(path + String.format(Locale.ENGLISH,"_%04d",count), format, grabResult,m_LogTarget) );
                } else
                {
                    if(wasRetrieveResultSucceeded) {
                        Log(LogLevel.Error, "Grab failed (grabResult are erroneous): " + grabResult.getErrorDescription());
                    }else{
                        Log(LogLevel.Error, "Grab failed (retrieveResult are erroneous)");
                    }
                    grabResult.release();
                }
                ++count;
            }
            // Tell the processor it can terminate when the work queue is empty.
            processor.stopWhenEmpty();
        }
        catch(Exception e){
            Log(LogLevel.Info, "Exception in getImage(): " + e.getMessage() );
        }
        finally {
            // Stop grabbing logic.
            //  Remember nothing is done if the Instant Camera is not currently grabbing.
            m_Camera.stopGrabbing();

            // Abort any work and wait for completion
            if( processor != null ) {
                processor.stopProcessing();
            }
        }

        return result;
    }

    /** Default constructor
     *  @param  logTarget Logger implementation can be null.
     **/
    PylonGrab( LogTarget logTarget) {
        m_LogTarget = logTarget;

        try {
            Log(LogLevel.Info, "Initialize pylon");
            pylon.pylonInitialize();

            // Get the first camera and create an InstantCamera helper class.
            Log(LogLevel.Info, "Create Instant Camera");
            m_Camera = new InstantCamera(TlFactory.getInstance().createFirstDevice());

            // Open camera. We keep the camera open the whole time.
            Log(LogLevel.Info, "Open camera");
            Log(LogLevel.Info, "Model Name (from enumeration): " + m_Camera.getDeviceInfo().getModelName());
            Log(LogLevel.Info, "Device Name (from enumeration): " + m_Camera.getDeviceInfo().getFullName());
            m_Camera.openCamera();
            Log(LogLevel.Info, "Vendor Name: " + m_Camera.getNodeMap().getStringNode("DeviceVendorName"));
            Log(LogLevel.Info, "Model Name: " + m_Camera.getNodeMap().getStringNode("DeviceModelName"));
            Log(LogLevel.Info, "Serial Number: " + m_Camera.getNodeMap().getStringNode("DeviceSerialNumber"));

        }
        catch(Exception e)
        {
            close();
            throw e;
        }
    }

    /** Prepare camera for image acquisition.
     **/
    public void prepareCamera()
    {
        // Set ROI:
        // On some cameras, the offsets are read-only.
        // Therefore we use the isWritable() method which also checks if the node is not null.
        IInteger offsetXNode = m_Camera.getNodeMap().getIntegerNode("OffsetX");
        IInteger offsetYNode = m_Camera.getNodeMap().getIntegerNode("OffsetY");
        if (genapi.isWritable(offsetXNode) && genapi.isWritable(offsetYNode)){
            long offsetXMin = offsetXNode.getMin();
            long offsetYMin = offsetYNode.getMin();
            offsetXNode.setValue(offsetXMin);
            offsetYNode.setValue(offsetYMin);
            Log(LogLevel.Info, "Set camera offsets to [" + offsetXMin + "," + offsetYNode + "]");
        }

        // Width and height are always writable.
        IInteger widthNode  = m_Camera.getNodeMap().getIntegerNode("Width");
        IInteger heightNode = m_Camera.getNodeMap().getIntegerNode("Height");
        long widthMax  = widthNode.getMax();
        long heightMax = heightNode.getMax();
        widthNode.setValue(widthMax);
        heightNode.setValue(heightMax);
        Log(LogLevel.Info, "Set image size to [" + widthMax + "," + heightMax + "]");

        // Try to set all auto functions to always on.
//        String [] autoNodeNames = {"ExposureAuto", "GainAuto", "BalanceWhiteAuto"};
//        for(String nodeName : autoNodeNames)
//        {
//            IEnumeration node = m_Camera.getNodeMap().getEnumerationNode(nodeName);
//            if( Objects.nonNull(node) ) {
//                node.trySetValue("Continuous");
//            }
//        }
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     */
    @Override
    synchronized public void close()
    {
        // Theoretically, close() can be called many times,
        //  but the call to PylonInitialize()/PylonTerminate()
        //  must be symmetric.
        if(m_wasTerminateCalled.get()) {
            return;
        }

        try {
            // Close the camera.
            //  If at this moment a thread is still waiting for new images, e.g., retrieveResult(),
            //   the call retrieveResult() will not wait for the timeout to expire, but will return imminently.
            //  Any GrabResult are still valid after closing the camera.
            //   This includes the data on it.
            if(m_Camera != null) {
                m_Camera.closeCamera();
            }

            // Frees up resources allocated by the pylon runtime system.
            //  PylonInitialize/PylonTerminate is reference counted.
            //   For every call of PylonInitialize(), a call to PylonTerminate() is required.
            //   The last call to PylonTerminate() will free up all resources.
            //  The InstantCamera calls inside the constructor PylonInitialize() to increase the counter
            //   and the finalizer(), called by the GC, will decrease it.
            //   You can trigger the clean up by calling .close()/.delete() on the InstantCamera.
            pylon.pylonTerminate(true);
            m_wasTerminateCalled.set(true);
        }
        catch( Exception e) {
            Log(LogLevel.Error, "Exception in close resource may leak. " + e.getMessage() );
        }
    }


    /***************** Utility ***********************/

    private AtomicBoolean m_wasTerminateCalled = new AtomicBoolean(false);
    private InstantCamera m_Camera;
    private LogTarget m_LogTarget;
    private void Log( LogTarget.LogLevel logLevel, String logText) {
        if( m_LogTarget != null) {
            m_LogTarget.Log(logLevel, logText);
        }
    }

    /** Get pixel formats supported by camera.
     *  Converts enumeration entries of the "PixelFormat" SFNC node to strings that can be set.
     *  Only supported pixel formats are returned.
     *  @return Pixel formats available on the camera.
     **/
    public List<String> getSupportedPixelFormats() {
        return m_Camera.getNodeMap().getEnumerationNode("PixelFormat").getSettableValues();
    }
    
    /** Find any supported BayerXX8 pixel format.
     * @return String containing the supported BayerXX8 pixel format.
     **/
    public String getSupportedBayer8Format() {
        for( String format : getSupportedPixelFormats() ) {
            if (format.matches("Bayer(RG|GR|BG|GB)8") ) {
                return format;
            }
        }
        return null;
    }
    
    /** Set pixel format on camera.
     **/
    public void setPixelFormat( String newFormat) {
        m_Camera.getNodeMap().getEnumerationNode("PixelFormat").setValue(newFormat);
    }
}
