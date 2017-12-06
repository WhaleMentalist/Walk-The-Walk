# Walk-The-Walk
Android application that tracks the number of steps you have taken. It will expand to competitions between other people with application. I also plan to add statistics. Since my own phone device does not contain the step-detector or the step-counter hardware I have to utilize the accelerometer. 

I have been following a particular repository and reading their description of how they use the accelerometer date to detect a step: https://github.com/danielmurray/adaptiv

It is very well done and JUST from their description I can reverse engineer a basic detection algorithm. I plan to figure out a means to conserve battery power when device is motion-less for a long period of time. As of now, the step detection works in a rather naive and basic way. It will not account for the user shaking the phone (i.e high frequency) and there are some instances of false positives (i.e insufficient threshold). For a rather simple project there are actually a lot of components needed to actually build this application.
