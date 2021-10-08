# Pylon Android for Basler Camera
Basler pylon sample code for Android application

**Developed by SODAVISION-TECH, using the pylon Software Camera Suite Java API by Basler AG**

# Version Information

This demo application has been built using the following tools:

    BuildToolsVersion: 29.0.3
    NDK:               23.0.7599858
    MinSdkVersion:     26
    TargetSdkVersion:  29
    Gradle Plugin:     4.2.2
    Gradle:            6.7.1
    CMake:             3.18.1
   
# RESTRICTIONS

* Only Basler USB3 Vision cameras are supported.
* To prevent image data loss, Basler recommends setting the MaxTransferSize streaming parameter to 16384   bytes. 
* Chunk and Event features are not supported.
* The Basler Compression Beyond feature in ace 2 Pro cameras is not supported.
* Basler recommends using smartphones and tablets with a USB 3.0 interface.
* Use Basler ace, ace 2, dart, or pulse USB 3.0 cameras with recommended embedded boards, e.g., Odroid    N2, NXP i.MX8.
* Android versions 8.0 or newer are supported. 
  For best performance, Basler recommends using Android versions 9.0 or newer.  
* Android versions 8.0 and 8.1 support a maximum USB request block size of 16384 bytes. 
* On Android versions 10 and newer, the camera serial number can't be used for enumeration filtering.
* Whenever accessing a Basler USB3 Vision camera from a pylon-based application, you will be asked by Android to grant access. 
  To avoid being asked for permission on every application launch, pylon offers a check box for enabling permanent access to any given camera.
* When using the pylon Java API to save images on an Android device, you may be asked to grant access rights. Please note that saving images in .jpg format is much 
  faster than image saving in .png file format.
* Images will be saved under Files > Images > Pictures.

# Sample APP in Android Application  

<a href="https://drive.google.com/uc?export=view&id=1LGGvMJ8tP1r96XPW7qOCAuzaOE3eIUoS"><img src="https://drive.google.com/uc?export=view&id=1LGGvMJ8tP1r96XPW7qOCAuzaOE3eIUoS" style="width: 377px; height: 800px" title="Baslerviewer app." />
</a>

# Available Functionalities:

This demo application offers some functions from the Basler pylon Camera Software Suite Java API.
This is a list of the supported functionality accesible through the demo application, which may depend on the exact camera model you are using:

**Feature list**
* Pixel Format.
* Acquisition frame rate.
* Exposure time.
* Gain.
* Auto White Balance.
* Save image in PNG or JPG format.
* Full screen live view mode.


