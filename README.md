# GpsSpoofReveal
OVERVIEW

GpsSpoofReveal is a project created by a computer engineering student from the University of Padua.  GpsSpoofReveal allows the detection of a Spoofing attack of the GNSS signal.

OPERATION

GpsSpoofReveal performs three types of checks:

•	presence of the navigation message in the GPS signal received,

•	visibility of satellites,

•	correctness of ephemeris.

These checks are made only to the satellites of the GPS constellation.

SERVER

For the correct functioning of the application it is necessary to create a server that sends the information related to the GPS constellation necessary for the correct functioning of the GpsSpoofReveal application. For a more detailed discussion consult the PDF, chapter 4 (!!! PDF is in Italian !!!).

FIXES AND IMPROVEMENTS

This application is still under development and may still have several bugs. (On certain devices/versions of Android GpsSpoofReveal may not work properly).

USEFUL LINKS

https://github.com/rastapasta/satellites-above

https://developer.android.com/guide/topics/sensors/gnss
