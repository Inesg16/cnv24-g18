## CNV Project - G18
## SpecialVFX@Cloud

Working directory: https://github.com/Inesg16/cnv24-g18 

This project contains three sub-projects:

1. `raytracer` - the Ray Tracing workload
2. `imageproc` - the BlurImage and EnhanceImage workloads
3. `webserver` - the web server exposing the functionality of the workloads

There is also the `javassist` containing the instrumentation code.


### How to build

1. `JAVA_HOME` environment variable set to Java 11+ distribution
2. `mvn clean package`

### How to run the project locally

1. Run the Web server with instrumentation using: 
```
java -cp "webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar" -javaagent:javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar=ICount:pt.ulisboa.tecnico.cnv.imageproc:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```

2. On another terminal, run one of the scripts `blurimage.sh`, `enhanceimage.sh`or `raytrace.sh`.

