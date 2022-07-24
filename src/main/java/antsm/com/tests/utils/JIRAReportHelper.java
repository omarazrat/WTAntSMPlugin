/*
 * Web application tester- Utility to test web applications via Selenium 
 * Copyright (C) 2021-Nestor Arias
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package antsm.com.tests.utils;

import antsm.com.tests.logic.JIRAReportInfo;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.plugins.AntSMUtilites.runTemplate;
import static antsm.com.tests.utils.JIRAReportHelper.TABLE_TYPE.COMPLETED_OUTSIDE_SPRINT;
import static antsm.com.tests.utils.JIRAReportHelper.TABLE_TYPE.REMOVED_FROM_SPRINT;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author nesto
 */
public final class JIRAReportHelper {

    private static Properties sysProps;
    private static Logger log = Logger.getLogger("WebAppTester");
    private static List<JIRAReportInfo> jirarepCache = new LinkedList<>();
    private static final String JIRA_USER, JIRA_PWD;

    public enum TICKET_STATE {
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        ON_HOLD
    }

    public enum TABLE_TYPE {
        COMPLETED_ISSUES,
        NOT_COMPLETED,
        REMOVED_FROM_SPRINT,
        COMPLETED_OUTSIDE_SPRINT;

        /**
         * Para reconocer el tipo de contenido según la frase
         */
        String getContent() {
            switch (this) {
                case COMPLETED_ISSUES:
                    return "Completed Issues";
                case NOT_COMPLETED:
                    return "Issues Not Completed";
                case REMOVED_FROM_SPRINT:
                    return "Issues Removed From Sprint";
                case COMPLETED_OUTSIDE_SPRINT:
                default:
                    return "Issues completed outside of this sprint";
            }
        }

        static TABLE_TYPE byHeader(String header) {
            for (TABLE_TYPE type : values()) {
                if (type.getContent().equals(header)) {
                    return type;
                }
            }
            return null;
        }
    }

    static {
        sysProps = getConfigFile();
        JIRA_USER = sysProps.getProperty("JIRA.user");
        JIRA_PWD = sysProps.getProperty("JIRA.password");
    }

    public static void login() throws InvalidVarNameException, IOException, InvalidParamException {
        runTemplate("loginJIRA.txt");
    }

    public static void logout() throws InvalidVarNameException, IOException, InvalidParamException {
        runTemplate("logoutJIRA.txt");
    }

    public static List<JIRAReportInfo> getJIRAReports(String teamName, String JIRAid, List<Integer> sprints) throws IOException, InvalidVarNameException, InvalidParamException {
        List<JIRAReportInfo> resp = new LinkedList<>();
        final String location = "https://jira.bbpd.io/secure/RapidBoard.jspa?rapidView={team}&view=reporting&chart=sprintRetrospective"
                .replace("{team}", JIRAid);
        AntSMUtilites.run("go={" + location + "}");
        AntSMUtilites.run("pause={\"time\":\"2 s\"}");
        final WebDriver driver = AntSMUtilites.getDriver();
        for (int sprint : sprints) {
            By pickerSelector = By.cssSelector("#ghx-chart-picker");
            if (driver.findElements(pickerSelector).isEmpty()) {
                log.severe("Couldn't find the selector of sprints in this page!");
                continue;
            }
            //Busca el selector de sprint...
            final WebElement wSelect = driver.findElement(pickerSelector);
            //#ghx-chart-picker
            Select select = new Select(wSelect);
            Predicate<WebElement> optFilter = opt -> {
                String text = opt.getAttribute("textContent");
//                log.info("testing opt: " + text);
                final boolean optResp = text.contains(teamName) && text.endsWith(" " + sprint);
//                if (optResp) {
//                    log.info("this is it!");
//                }
                return optResp;
            };
            final Optional<WebElement> match = select.getOptions().parallelStream().filter(optFilter).findFirst();
            if (!match.isPresent()) {
                log.log(Level.SEVERE, "Couldn't find sprint " + sprint + " of team " + teamName);
                continue;
            }
            WebElement opt = match.get();
            AntSMUtilites.run("go={" + location + "&sprint=" + opt.getAttribute("value") + "}");

            AntSMUtilites.run("pause={\"time\":\"2 s\"}");
            JIRAReportInfo jiraRInfo = new JIRAReportInfo(teamName, sprint);
            int toDo = 0, inProgress = 0, inReview = 0, onHold = 0, totalTickets = 0, completedTickets = 0;
            try {
                List<TABLE_TYPE> tableTypes = new LinkedList<>();
                By headerSelector = By.tagName("h4");
                List<WebElement> headers = driver.findElements(headerSelector);
                if (headers.isEmpty()) {
                    log.severe("no heaers found in report page!");
                    continue;
                }
                //Encabezados de tablas
                for (WebElement header : headers) {
                    String headerText = header.getText();
                    TABLE_TYPE type = TABLE_TYPE.byHeader(headerText);
                    if (type != null) {
                        tableTypes.add(type);
                    }
//                    log.log(Level.INFO, "header:{0}", headerText);
                }
                //Toma las estadísticas de tablas 1 a 1
                int counter = 0;
                By tableSelector = By.cssSelector("table.aui");
                List<WebElement> tables = driver.findElements(tableSelector);
                for (int i = 0; i < tables.size(); i++) {
                    WebElement table = tables.get(i);
                    TABLE_TYPE tableType = tableTypes.get(i);
//                    log.info("table of type "+tableType);
                    if(tableType==null){
                        log.log(Level.SEVERE, "table not matching any type found with index {0}{1}!", new Object[]{i, 1});
                        continue;
                    }
                    if(tableType.equals(COMPLETED_OUTSIDE_SPRINT)){
                        log.log(Level.INFO, "skipping {0} table. Team {1} sprint {2}", new Object[]{tableType.getContent(), teamName, sprint});
                        continue;
                    }
                    int numRows = getTableRows(table);
                    totalTickets += numRows;
                    switch (tableType) {
                        case COMPLETED_ISSUES:
                            completedTickets += numRows;
                            break;
                        case NOT_COMPLETED:
                        case REMOVED_FROM_SPRINT:
//                        case COMPLETED_OUTSIDE_SPRINT:
                            toDo += getTableRows(table, TICKET_STATE.TODO);
                            inProgress += getTableRows(table, TICKET_STATE.IN_PROGRESS);
                            inReview += getTableRows(table, TICKET_STATE.IN_REVIEW);
                            onHold += getTableRows(table, TICKET_STATE.ON_HOLD);
                            break;
                    }
                }
            } finally {
                jiraRInfo.setCompletedTickets(completedTickets);
                jiraRInfo.setInProgress(inProgress);
                jiraRInfo.setInReview(inReview);
                jiraRInfo.setOnHold(onHold);
                jiraRInfo.setToDo(toDo);
                jiraRInfo.setTotalTickets(totalTickets);
                resp.add(jiraRInfo);
                jirarepCache.add(jiraRInfo);
            }
        }
        return resp;
    }

    public static boolean inJIRARCache(String teamName, Integer sprint) {
        JIRAReportInfo report = new JIRAReportInfo(teamName, sprint);
        return jirarepCache.contains(report);
    }

    /**
     * PRE: el reporte buscado ya está en {@link #jirarepCache}
     *
     * @param teamName
     * @param sprints
     * @return
     */
    public static List<JIRAReportInfo> getJIRARCache(String teamName, List<Integer> sprints) {
        List<JIRAReportInfo> resp = new LinkedList<>();
        for (Integer sprint : sprints) {
            JIRAReportInfo sample = new JIRAReportInfo(teamName, sprint);
            Optional<JIRAReportInfo> jiraMatch = jirarepCache.stream().filter(r -> r.equals(sample))
                    .findFirst();
            resp.add(jiraMatch.get());
        }
        return resp;
    }

    public static int getTableRows(WebElement table) {
        return getTableRows(table, null);
    }

    public static int getTableRows(WebElement table, TICKET_STATE desiredStatus) {
        List<WebElement> rows = table.findElements(By.tagName("tr"));
        if (desiredStatus == null) {
            return rows.size() - 1;
        }
        int total = 0;
        //  Key	Summary	Issue Type	Priority	Status	Story Points (7)
        for (int i = 1; i < rows.size(); i++) {
            WebElement row = rows.get(i);
            By statusSelector = By.cssSelector("td:nth-child(5) > span");
            boolean statusAvailable = !row.findElements(statusSelector).isEmpty();
            if (!statusAvailable) {
                continue;
            }
            WebElement statusCell = row.findElement(statusSelector);
            String status = statusCell.getText();
            String targetStatus;
            switch (desiredStatus) {
                case IN_PROGRESS:
                    targetStatus = "In Progress";
                    break;
                case IN_REVIEW:
                    targetStatus = "In Progress";
                    break;
                case ON_HOLD:
                    targetStatus = "On Hold";
                    break;
                case TODO:
                    targetStatus = "TO DO";
            }
//            log.log(Level.INFO, "got: {0}, desired: {1}.", new Object[]{status, desiredStatus});
            if (!status.equals(desiredStatus)) {
                continue;
            }
            total++;
        }
        return total;
    }

    /**
     * Llena los datos desde una tabla de HTML tomada de un reporte de JIRA
     * (sprint report)
     *
     * @param jiraRInfo
     * @param table
     */
    public static double getTablePoints(WebElement table) {
        return getTablePoints(table, null);
    }

    public static double getTablePoints(WebElement table, TICKET_STATE desiredStatus) {
        double total = 0;
        List<WebElement> rows = table.findElements(By.tagName("tr"));
        //  Key	Summary	Issue Type	Priority	Status	Story Points (7)
        for (int i = 1; i < rows.size(); i++) {
            WebElement row = rows.get(i);
            if (desiredStatus != null) {
                WebElement statusCell = row.findElement(By.cssSelector("td:nth-child(5) > span"));
                String status = statusCell.getText();
                String targetStatus;
                switch (desiredStatus) {
                    case IN_PROGRESS:
                        targetStatus = "In Progress";
                        break;
                    case IN_REVIEW:
                        targetStatus = "In Progress";
                        break;
                    case ON_HOLD:
                        targetStatus = "On Hold";
                        break;
                    case TODO:
                        targetStatus = "TO DO";
                }
                log.log(Level.INFO, "got: {0}, desired: {1}.", new Object[]{status, desiredStatus});
                if (!status.equals(desiredStatus)) {
                    continue;
                }
            }
            WebElement pointsCell = row.findElement(By.cssSelector("td:nth-child(6)"));
            total += Double.parseDouble(pointsCell.getText());
        }
        return total;
    }

    public static boolean inJIRARCache(String teamName, List<Integer> sprints) {
        for (Integer sprint : sprints) {
            if (!inJIRARCache(teamName, sprint)) {
                return false;
            }
        }
        return true;
    }

    public static String getJIRAName(String teamName) {
        String key = "team." + teamName + ".JIRA.id";
//        log.info("finding key " + key);
        final String resp = sysProps.getProperty(key);
        return resp;
    }

    public static String getJiraUser() {
        return JIRA_USER;
    }

    public static String getJiraPassword() {
        return JIRA_PWD;
    }
}
