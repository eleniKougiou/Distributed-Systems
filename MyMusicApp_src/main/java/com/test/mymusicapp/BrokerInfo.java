package com.test.mymusicapp;

import java.io.Serializable;

public class BrokerInfo implements Serializable {
    private int portP, portC;
    private String ip;

    public BrokerInfo(){

    }

    public BrokerInfo(String ip, int portC){
        this.ip = ip;
        this.portC = portC;
    }

    public BrokerInfo(String ip, int portP, int portC){
        this.ip = ip;
        this.portP = portP;
        this.portC = portC;
    }

    public String getIp(){
        return ip;
    }

    public void setIp(String ip){
        this.ip = ip;
    }

    public int getPortC(){
        return portC;
    }

    public void setPortC(int portC){
        this.portC = portC;
    }

    public int getPortP(){
        return portP;
    }

    public void setPortP(){
        this.portP = portP;
    }

    public String getInfo (){
        return "Ip: " + ip + " & Port: " + portC;
    }
}

