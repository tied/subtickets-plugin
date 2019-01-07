package com.subtickets.servlet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType()
@XmlRootElement(name = "jira-database-config")
public class JiraDBConfig {

    @XmlElement(name = "database-type")
    public String databaseType;

    @XmlElement(name = "jdbc-datasource")
    public DataSourceConfig dataSourceConfig;

}