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
package antsm.com.tests.tests;

import antsm.com.tests.plugins.AntSMUtilites;
import oa.com.tests.actionrunners.interfaces.tests.PluginInterfaceEmulator;
import antsm.com.tests.utils.JIRAReportHelper;
import static antsm.com.tests.utils.JIRAReportHelper.TABLE_TYPE.COMPLETED_OUTSIDE_SPRINT;
import static antsm.com.tests.utils.JIRAReportHelper.getTableRows;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author nesto
 */
public class JiraReportHelperTester extends PluginInterfaceEmulator {

    private Logger log = Logger.getLogger("WebAppTester");

    public JiraReportHelperTester() {
    }

//    @Test
    public void testBoardType() throws InvalidParamException, Exception {
        for (int JIRAid : new Integer[]{2605, 1709}) {
            log.log(Level.INFO, "type of board {0}: {1}", new Object[]{JIRAid, JIRAReportHelper.getBoardType(JIRAid)});
        }
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    
    public void test() throws MalformedURLException, InterruptedException {
        String slash = System.getProperty("file.separator");
        File f = new File("src" + slash + "main" + slash + "resources" + slash + "files" + slash + "testBoard.html");
        URL url = f.toURI().toURL();
        go(url.toString());
        Thread.sleep(1700);
        WebDriver driver = AntSMUtilites.getDriver();
        List<JIRAReportHelper.TABLE_TYPE> tableTypes = new LinkedList<>();
        By headerSelector = By.tagName("h4");
        List<WebElement> headers = driver.findElements(headerSelector);
        Assert.assertFalse(headers.isEmpty());
        //Encabezados de tablas
        int totalTickets = 0, completedTickets = 0, toDo = 0, inProgress = 0, inReview = 0, onHold = 0;
        for (WebElement header : headers) {
            String headerText = header.getText();
            JIRAReportHelper.TABLE_TYPE type = JIRAReportHelper.TABLE_TYPE.byHeader(headerText);
            if (type != null) {
                tableTypes.add(type);
            }
//                    log.log(Level.INFO, "header:{0}", headerText);
        }

        By tableSelector = By.cssSelector("table.aui");
        List<WebElement> tables = driver.findElements(tableSelector);
        for (int i = 0; i < tables.size(); i++) {
            WebElement table = tables.get(i);
            JIRAReportHelper.TABLE_TYPE tableType = tableTypes.get(i);
//                    log.info("table of type "+tableType);
            if (tableType == null) {
                log.log(Level.SEVERE, "table not matching any type found with index {0}{1}!", new Object[]{i, 1});
                continue;
            }
            if (tableType.equals(COMPLETED_OUTSIDE_SPRINT)) {
                log.log(Level.INFO, "skipping {0} table.", new Object[]{tableType.getContent()});
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
                    toDo += getTableRows(table, JIRAReportHelper.TICKET_STATE.TODO);
                    inProgress += getTableRows(table, JIRAReportHelper.TICKET_STATE.IN_PROGRESS);
                    inReview += getTableRows(table, JIRAReportHelper.TICKET_STATE.IN_REVIEW);
                    onHold += getTableRows(table, JIRAReportHelper.TICKET_STATE.ON_HOLD);
                    break;
            }
        }
        log.log(Level.INFO, "total:{0},completed:{1},todo:{2},in progress:{3},in review:{4},on hold:{5}", new Object[]{totalTickets, completedTickets, toDo, inProgress, inReview, onHold});
    }
}
