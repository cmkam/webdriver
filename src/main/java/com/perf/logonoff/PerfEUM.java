package com.perf.logonoff;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Perf;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Created on 2019/11/25
 *
 * @author: 45073953
 */
class MyThread extends Thread{
    private static Logger perfLog = LoggerFactory.getLogger("dbb.PERF");
    private static Logger logger = LoggerFactory.getLogger(MyThread.class);

    private int iteration;
    private int sleepSecond;
    private String finishTime;
    private int proxyPeriod;

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }
    LogonAndOff logonAndOff = new LogonAndOff();

    public MyThread(String name, int iteration, int sleepSecond, boolean headLess, int proxyPeriod, String finishTime) {
        logonAndOff.setBrowserType(name);
        setIteration(iteration);
        this.sleepSecond = sleepSecond;
        this.finishTime = finishTime;
        this.proxyPeriod = proxyPeriod;
        logonAndOff.setHeadLess(headLess);
    }


    public void run(){
        if (iteration == -1) iteration = Integer.MAX_VALUE;
        System.out.println("iteration=" + iteration);
        logger.info("Count={}",iteration);

        for(int i=1; i<=iteration; i++){
            logonAndOff.setId(i);
            if (i%2==0){
                logonAndOff.setUrl(LogonAndOff.url1);
            }else{
                logonAndOff.setUrl(LogonAndOff.url2);
            }

            perfLog.info("[START]{}|{}|{}|",
                    logonAndOff.getBrowserType(),logonAndOff.getDisplayUrl(), ""+i);

            if (proxyPeriod == -1){
                logonAndOff.setUseProxy(false);
                logonAndOff.setProxy(null);
            }else{
                if (  i <= 2 ||
                        i%proxyPeriod==0 || (i+1)%proxyPeriod==0){
                    logonAndOff.setUseProxy(true);
                    logonAndOff.setProxy(getProxyServer());
                }else{
                    logonAndOff.setUseProxy(false);
                    logonAndOff.setProxy(null);
                }
            }
            logonAndOff.initDriver();

            String now = null;
            if (finishTime!=null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddHHmm");
                now = LocalDateTime.now().format(formatter);
                if (finishTime.compareTo(now) < 0) {
                    perfLog.info("[STOP] finishTime={}, now={}", finishTime, now);
                    logonAndOff.quit();
                    return;
                }

            }


            try{
                logonAndOff.logon();

                perfLog.info("[END]{}|{}|{}| S={}, H={} {} {}",
                        logonAndOff.getBrowserType(),logonAndOff.getDisplayUrl(), ""+i,
                        logonAndOff.getDbbServerName(),logonAndOff.getHarFileName(),
                        (finishTime==null?"":",finishTime="+finishTime),
                        (now==null?"":",now="+ now ));

                logonAndOff.logOff();
            } catch (Exception e){
                logger.error("err",e);
                if(!logonAndOff.isDriverAlive()){
                    logonAndOff.initDriver();
                }
            }

            logonAndOff.sleep(sleepSecond);

            logonAndOff.quit();
        }
    }

    public BrowserMobProxy getProxyServer() {
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
// above line is needed for application with invalid certificates
        proxy.start();
        return proxy;
    }



}


public class PerfEUM {
    private static Logger logger = LoggerFactory.getLogger(PerfEUM.class);
    private static Logger perfLog = LoggerFactory.getLogger("dbb.PERF");
    public static void main(String[] args) throws ParseException {
        perfLog.info("Version:{}",PerfEUM.class.getPackage().getImplementationVersion());
        int iteration = Integer.parseInt(args[0]);

        int sleepSecond = Integer.parseInt(args[1]);

        boolean headLess = Boolean.parseBoolean(args[2]);

        int proxyPeriod = Integer.parseInt(args[3]);

        String finishTime = finishTime = args[4];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        finishTime = LocalDateTime.now().format(formatter)+finishTime;

        logger.info("URL[{}]\r\n[{}]", args[5], args[6]);

        int pageLoadTimeout = 120;// default to 60s
        if (args.length>10){
            pageLoadTimeout = Integer.parseInt(args[10]);
        }

        int loginDelay = 0;// default to 0s
        if (args.length>11){
            loginDelay = Integer.parseInt(args[11]);
        }

        MyThread t1 = new MyThread("chrome", iteration, sleepSecond, headLess, proxyPeriod, finishTime);
        t1.logonAndOff.initSetup(args[5], args[6], args[7], args[8], args[9], pageLoadTimeout, loginDelay);
        t1.start();

        //MyThread t2 = new MyThread("firefox", iteration);
        //t2.start();

        //MyThread t3 = new MyThread("ie", iteration);
        //t3.start();
    }
}