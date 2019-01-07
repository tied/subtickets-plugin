package com.subtickets.servlet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType()
public class DataSourceConfig {

    @XmlElement(name = "url")
    public String url;

    @XmlElement(name = "driver-class")
    public String driveClassName;

    @XmlElement(name = "username")
    public String username;

    @XmlElement(name = "password")
    public String password;

}

