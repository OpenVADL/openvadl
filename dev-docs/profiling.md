# Java Profiler Setup and Use

Using a Profiler is a necessary step when trying to optimize a large piece of
software such as OpenVADL. The setup necessary to use them is however not
always trivial, or at least not easy to find. This file's purpose is then to
collect information regarding the setup and use of profilers for developing
OpenVADL.

There are of course many Java profilers and even more setups for them. So far
we only document the basic setup and use of the profiler integrated in IntelliJ
IDEA. If you have experience using alternative java profilers, please add a
section to this file so that other developers may also benefit.



## IntelliJ IDEA's Integrated Profiler

1. Go to Settings -> Build, Execution, Deployment -> Java Profiler.
2. Click the "Async Profiler" button. This creates a new Profiler Profile.
    1. Set "Agent Options" to `event=alloc,interval=1`. This will be your
       memory allocation profiler.
    2. Set a name of your choosing.
3. Click "Async Profiler" again to create another Profiler Profile
    1. Set "Agent Options" to `event=wall,interval=200us,jfrsync=profile`. This
       will be your CPU profiler
    2. Set a name of your choosing.
4. Leave the settings by clicking "Ok"
5. Click the three dots next to the "Run" button in the top right.
6. Click "Profile <your-target> with <your-profiler>".
