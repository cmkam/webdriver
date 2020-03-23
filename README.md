# webdriver

Download and install JAVA 1.8 if you did not install.

```console
java -jar DBB-jar-with-dependencies.jar <No of execution> <delay before next page> <hidden mode>
<No of execution between Har file generation> <Time for auto shutdown, HHMM> 
<url1> <url2> <username> <password> <code> 
```

|no|arg|comment|
| :------------- |:-------------:| :-----|
|0|No of execution|  -1 means unlimited  |
|1|delay before next page in second| 180s = 3min|
|2|hidden mode| hidden mode will create abnormal user agent to appD, not recommend if possible|
|3|No of execution between Har file generation| 20 means every 20 time normal capture, both HK and CN site will capture HAR  once, -1 will disable the proxy and HAR generation|  
|4|Time for auto shutdown, HHMM| 2359 means server will be stop after 23:59|
|5|url1| first url, url will switch between first and second|
|6|url2| second url||  
|7|username|| 
|8|password||
|9|code||  

# Development
Rebuild command  
mvn clean package
