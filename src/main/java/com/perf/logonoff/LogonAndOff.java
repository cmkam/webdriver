package com.perf.logonoff;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019/11/18
 *
 * @author: 45073953
 */
public class LogonAndOff {
    private static Logger perfLog = LoggerFactory.getLogger("dbb.PERF");
    private static Logger logger = LoggerFactory.getLogger(LogonAndOff.class);


    public static String url1;
    public static String url2;

    private String username;
    private String password;
    private String securityCode;

    private WebDriver driver = null;
    private String browserType; // chrome, firefox, ie

    private String url;
    private BrowserMobProxy proxy;
    private boolean useProxy;
    private int pageLoadTimeout;
    private int loginDelay;

    public void initSetup(String url1, String url2, String username, String password, String securityCode, int pageLoadTimeout, int loginDelay) {
        this.url1 = url1;
        this.url2 = url2;
        this.username= username;
        this.password=password;
        this.securityCode = securityCode;
        this.pageLoadTimeout = pageLoadTimeout;
        this.loginDelay = loginDelay;
    }

    public void setUseProxy(boolean useProxy){
        this.useProxy = useProxy;
    }
    public boolean getUseProxy(){return useProxy;}

    public void setProxy(BrowserMobProxy proxy){this.proxy=proxy;}

    public void setUrl(String url){
        this.url = url;
    }

    public String getUrl(){
        return url;
    }

    public String getBrowserType() {
        return browserType;
    }


    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }

    public static void main(String[] args) {
        LogonAndOff logonAndOff = new LogonAndOff();
        logonAndOff.browserType = args[0].toLowerCase();
        int iteration = Integer.parseInt(args[1]);
        //int sleepSecond = Integer.parseInt(args[2]);

        logonAndOff.initDriver();

        for (int i = 1; i <=iteration; i++) {             
            logger.info("[" + logonAndOff.browserType + "]: Test EUM for "+Integer.toString(i) + "' around ...");
            try {
                logonAndOff.logon();
                logonAndOff.logOff();
            } catch (Exception e) {
                logger.error("err",e);
                if (!logonAndOff.isDriverAlive()) {
                    logonAndOff.initDriver();
                }
            }
            
        }
        logonAndOff.quit();
    }

    public void initDriver() {
        switch (browserType) {
            case "chrome":
                initChrome();
                break;
            case "firefox":
                initFirefox();
                break;
            case "ie":
                initIE();
                break;
            default: {
            }
        }
    }


    public boolean isDriverAlive() {
        try {
            if (driver.getTitle().equals("")) {
                return false;
            }
        } catch (Exception ex) {
            logger.info("[" + browserType + "] is closed accidentally, relaunch again.");
            return false;
        }
        return true;
    }

    public void initChrome() {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (proxy!=null) {
            Proxy seleniumProxy = getSeleniumProxy(proxy);
            chromeOptions.setCapability(CapabilityType.PROXY, seleniumProxy);
        }

        if (OsUtils.isWindows()) {
            System.setProperty("webdriver.chrome.driver", "drivers\\chromedriver.exe");
        }else {
            System.setProperty("webdriver.chrome.driver", "drivers/chromedriver");
        }

        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("test-type");
        chromeOptions.addArguments("ignore-certificate-errors");
        chromeOptions.setAcceptInsecureCerts(true);
//        if (agent!=null) {
//            chromeOptions.addArguments("user-agent=\""+agent+"\"");
//        }

 //       boolean headLess = false;
        if (headLess) {
            chromeOptions.setHeadless(true);
            chromeOptions.addArguments("--window-size=1920x1080");
        } else {
            chromeOptions.addArguments("--start-maximized");
        }

        ChromeDriver cdriver = new ChromeDriver(chromeOptions);
        cdriver.manage().timeouts().implicitlyWait(240, TimeUnit.SECONDS);
        cdriver.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
        //slowChrome(cdriver);
        driver = cdriver;
    }

    private void slowChrome(ChromeDriver cdriver){
        CommandExecutor executor = cdriver.getCommandExecutor();

        Map map = new HashMap();
        map.put("offline", false);
        map.put("latency", 5);

        map.put("download_throughput", 25600);
        map.put("upload_throughput", 51200);
        try {
            Response response = executor.execute(
                    new Command(cdriver.getSessionId(),
                            "setNetworkConditions",
                            ImmutableMap.of("network_conditions", ImmutableMap.copyOf(map))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initFirefox() {
        System.setProperty("webdriver.gecko.driver", "src/test/resources/drivers/eckodriver.exe");
        driver = new FirefoxDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    public void initIE() {
        System.setProperty("webdriver.ie.driver", "src\\test\\resources\\drivers\\IEDriverServer.exe");
        InternetExplorerOptions options = new InternetExplorerOptions();
        options.ignoreZoomSettings();
        driver = new InternetExplorerDriver(options);
        driver.manage().window().maximize();
    }

    public Proxy getSeleniumProxy(BrowserMobProxy proxyServer) {
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxyServer);
        try {
            String hostIp = Inet4Address.getLocalHost().getHostAddress();
            seleniumProxy.setHttpProxy(hostIp+":" + proxyServer.getPort());
            seleniumProxy.setSslProxy(hostIp+":" + proxyServer.getPort());


        } catch (UnknownHostException e) {
            logger.error("invalid Host Address", e);
        }
        return seleniumProxy;
    }

    private String dbbServerName;
    private String harFileName;
    private int id;
    private boolean headLess = false;

    public void setHeadLess(boolean headless){
        this.headLess = headless;
    }

    public void setId(int id){this.id = id;}

    public String getDbbServerName() {
        return dbbServerName;
    }
    public String getHarFileName(){
        return harFileName;
    }

    public double logon() {

        if (proxy!=null){
            //proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
            proxy.enableHarCaptureTypes(CaptureType.REQUEST_HEADERS,
                    CaptureType.REQUEST_CONTENT,
                    CaptureType.REQUEST_BINARY_CONTENT,
                    CaptureType.REQUEST_COOKIES,
                    CaptureType.RESPONSE_HEADERS,
                    CaptureType.RESPONSE_CONTENT,
                    CaptureType.RESPONSE_BINARY_CONTENT,
                    CaptureType.RESPONSE_COOKIES);
            proxy.newHar();
        }

        driver.get(url);

        dbbServerName="NA";
        {

            try {
                Object response = ((JavascriptExecutor) driver).executeAsyncScript(
                        "var callback = arguments[arguments.length - 1];" +
                                "var xhr = new XMLHttpRequest();" +
                                "xhr.open('GET', '/portalserver/services/rest/logon/config', true);" +
                                "xhr.onreadystatechange = function() {" +
                                "  if (xhr.readyState == 4) {" +
                                "    callback(xhr.getResponseHeader('S')+';');" +
                                "  }" +
                                "};" +
                                "xhr.send();");
                if (response != null) {
                    dbbServerName = response.toString();
                    String[] result = dbbServerName.split(" ");
                    dbbServerName = result[result.length - 1].replace(";","");
                }
            }catch (Exception e){logger.error("err",e);}
        }
        //String userAgent = (String) ((JavascriptExecutor) driver).executeScript("return navigator.userAgent;");


        System.out.println(new Date()+";"+url+",dbbServerName="+dbbServerName);

        {
            WebDriverWait wait = new WebDriverWait(driver, pageLoadTimeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("userName")));

            driver.findElement(By.id("userName")).sendKeys(username);
        }

        {
            WebDriverWait wait = new WebDriverWait(driver, pageLoadTimeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Next']")));

            driver.findElement(By.xpath("//*[text()='Next']")).click();
        }

        {
            WebDriverWait wait = new WebDriverWait(driver, pageLoadTimeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
            driver.findElement(By.id("password")).sendKeys(password);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("security-code")));
            driver.findElement(By.id("security-code")).sendKeys(securityCode);
        }
        long beforeLogonTime;
        {
            WebDriverWait wait = new WebDriverWait(driver, pageLoadTimeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Log on']")));
            sleep(loginDelay);
            driver.findElement(By.xpath("//*[text()='Log on']")).click();
            beforeLogonTime = System.currentTimeMillis();;

        }



        try {
            skipQuickTour();
            if (driver.getTitle().equals("Business Internet Banking")) {
                logger.info("[" + browserType + "]: Logon OK.");
            }
        } catch (Exception e) {
            logger.error("Err ",e);
        }

        double timeUsedForLogon;
        {
            WebDriverWait wait = new WebDriverWait(driver, pageLoadTimeout);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='account-row ng-scope other']")));
            timeUsedForLogon = ((double)(System.currentTimeMillis() - beforeLogonTime))/1000;

            captureHAR();
        }

        return timeUsedForLogon;
    }

    private void captureHAR(){
        if (proxy ==null){
            harFileName = null;
        }else{
            try{
                Har har = proxy.getHar();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                harFileName = LocalDateTime.now().format(formatter) + "_"+id+"_"+getDisplayUrl()+"_"+dbbServerName+".har";
                File harFile = new File("log/"+harFileName);
                //harFile.mkdirs();
                harFile.createNewFile();

                har.writeTo(harFile);
            } catch (IOException e) {
                logger.error("Err ",e);
            } catch (Exception e){
                logger.error("Err ",e);
            }
//   List<HarEntry> entries = proxy.getHar().getLog().getEntries();
//            for(HarEntry entry :entries){
//                System.out.println(entry.getRequest().getUrl());
//            }
        }
    }

    public void skipQuickTour() {
//        if (driver.getPageSource().contains("Would you like a 1 minute quick tour")){
        {
            WebDriverWait wait = new WebDriverWait(driver, 120);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='introjs-tooltiptext ng-scope']")));
            //wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("account-box ng-scope")));
            if (driver.getPageSource().split("Would you like a 1 minute quick tour").length == 5) {
                // occurred 4 times
                driver.findElements(By.xpath("//*[text() = 'No, please skip.']")).get(1).click();
               // sleep(1);
            }
        }

        {
            WebDriverWait wait = new WebDriverWait(driver, 120);
            sleep(2);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='introjs-tooltiptext ng-scope']")));
            //if (driver.getPageSource().split("Show quick tour again next time I log on").length == 5) {
                // occurred 4 times
            //driver.findElement(By.xpath("//button[@class='btn btn-primary ng-isolate-scope']")).click();
                driver.findElements(By.xpath("//button[@data-analytics-event-content='quick tour - step 6 - complete']")).get(1).click();
            //}
        }
    }

    public void logOff() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("scroll(0,0)");
        driver.findElement(By.xpath("//*[text()='Log off']")).click();
        sleep(3);
        if (driver.getPageSource().contains("You have successfully logged off your Business Internet Banking session")) {
            logger.info("[" + browserType + "]: Logoff OK.");
        }
    }

    public void quit() {
        if (proxy != null) {
            proxy.stop();
        }
        driver.quit();
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
            //Thread.sleep(seconds * 0);
        } catch (InterruptedException e) {
            logger.error("err",e);
        }
    }

    public String getDisplayUrl(){
        int i= url.lastIndexOf(".");
        int j = url.indexOf("/",i);
        return url.substring(i+1,j);
    }
}

final class OsUtils {
    private static String OS = null;
    public static String getOsName()
    {
        if(OS == null) { OS = System.getProperty("os.name"); }
        return OS;
    }
    public static boolean isWindows()
    {
        return getOsName().startsWith("Windows");
    }

}