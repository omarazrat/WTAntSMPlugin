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

import antsm.com.tests.logic.AbstractTeamSprintInfo;
import antsm.com.tests.logic.CapacityInfo;
import antsm.com.tests.logic.JIRAReportInfo;
import antsm.com.tests.logic.SPReportInfo;
import antsm.com.tests.logic.Ticket;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacities;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacitiesCache;
import static antsm.com.tests.utils.ConfluenceHelper.inCapacitiesCache;
import static antsm.com.tests.utils.JIRAReportHelper.collectBugBashes;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAName;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRARCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAReports;
import static antsm.com.tests.utils.JIRAReportHelper.inJIRARCache;
import static antsm.com.tests.utils.PythonReportHelper.getSPRCache;
import static antsm.com.tests.utils.PythonReportHelper.getSPReports;
import static antsm.com.tests.utils.PythonReportHelper.inSPRCache;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import javax.swing.SwingWorker;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author nesto
 */
public final class PoiHelper {

    private static Logger log = Logger.getLogger("WebAppTester");
    private static Properties sysProps;
    private static int year;
    private static HashMap<Integer, Integer> sprintxquarter = new HashMap<>();
    private static HashMap<Integer, String> quarterQC = new HashMap<>();

    public static void init() {
        sysProps = getConfigFile();
        fillProfileInfo();
        year = Integer.parseInt(sysProps.getProperty("year", "0"));
//        log.info("year:" + year);
    }

    public static void destroy() {
        sprintxquarter.clear();
    }

    public static Workbook buildDatabaseWB(String destPath, List<String> teamNames, List<Integer> sprints, SwingWorker worker) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        buildDatabase(workbook, teamNames, sprints, worker);
        FileOutputStream out = new FileOutputStream(destPath);
        workbook.write(out);
        workbook.close();
        out.close();
        worker.firePropertyChange("progress", 95, 100);
        return workbook;
    }

    private static Sheet buildDatabase(Workbook wb, List<String> teamNames, List<Integer> sprints, SwingWorker worker) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        int rownum = 0, cellnum = 0;
        Sheet sheet = wb.createSheet("Data Base");
        Row row = sheet.createRow(rownum++);
        //headers
        for (String header
                : new String[]{"Team",
                    "Year", "Quarter", "Sprint", "Estimated", "Completed", "Removed", "Added", "Story Points Variance",
                    "Completion Ratio", "Total Tickets", "Completed Tickets", "Effective hours", "To Do", "In Progress", "In Review", "On hold", "Developers per sprint", "Team Coefficient"}) {
            Cell cell = row.createCell(cellnum++);
            IndexedColors color = IndexedColors.BRIGHT_GREEN;
            cell.setCellValue(header);
            XSSFCellStyle cellStyle = buildHeaderStyle(color);
            cell.setCellStyle(cellStyle);
        }

        sheet.createFreezePane(1, 1);
        try {
            worker.firePropertyChange("progress", 0, 5);
            int delta = 90 / teamNames.size();
//            log.info("delta:"+delta);
            int total = 5;
            for (String teamName : teamNames) {
                worker.firePropertyChange("progress", total - delta, total);
                total += delta;
                //Bajemos el capacity...
                final List<CapacityInfo> capacities
                        = inCapacitiesCache(teamName, sprints)
                        ? getCapacitiesCache(teamName, sprints)
                        : getCapacities(teamName, sprints);
//                log.info("capacities found:");
//                capacities.forEach(c -> log.info(c.toString()));
//                log.info("");
                //Reporte de Confluence...
                String JIRAid = getJIRAName(teamName);
                if (JIRAid == null) {
                    log.log(Level.SEVERE, "couldn''t find JIRA id for team {0}", teamName);
                    continue;
                }
                final List<SPReportInfo> spreports
                        = inSPRCache(teamName, sprints)
                        ? getSPRCache(teamName, sprints)
                        : getSPReports(teamName, JIRAid, sprints);
//                log.info("spreports found:");
//                spreports.forEach(s -> log.info(s.toString()));
//                log.info("");
                //Reporte de JIRA
                final List<JIRAReportInfo> JIRAReports
                        = inJIRARCache(teamName, sprints)
                        ? getJIRARCache(teamName, sprints)
                        : getJIRAReports(teamName, JIRAid, sprints);
//                log.info("jira reports found:");
//                JIRAReports.forEach(s -> log.info(s.toString()));
//                log.info("");
                for (Integer sprint : sprints) {
                    cellnum = 0;
//                    "Team","Year", "Quarter", "Sprint", "Estimated", "Completed", "Removed", "Added", "Story Points Variance",
//                     "Completion Ratio", "Total Tickets", "Completed Tickets", "Effective hours", "To Do", "In Progress", "In Review", "On hold", "Developers per sprint", "Team Coefficient"
                    Predicate<AbstractTeamSprintInfo> teamSprintSearcher = (x -> x.getTeamName().equals(teamName) && x.getSprint() == sprint);
                    Optional<CapacityInfo> capacityMatch = capacities.stream().filter(teamSprintSearcher).findFirst();
                    CapacityInfo capacityInfo = null;
                    if (capacityMatch.isEmpty()) {
                        log.severe("skipping capacity not found: " + teamName + ", sprint " + sprint);
                    } else {
                        capacityInfo = capacityMatch.get();
                    }
                    Optional<SPReportInfo> SPRMatch = spreports.stream().filter(teamSprintSearcher).findFirst();
                    SPReportInfo spreportInfo = null;
                    if (SPRMatch.isEmpty()) {
                        log.severe("skipping python report not found: " + teamName + ", sprint " + sprint);
                    } else {
                        spreportInfo = SPRMatch.get();
                    }
                    Optional<JIRAReportInfo> JIRAMatch = JIRAReports.stream().filter(teamSprintSearcher).findFirst();
                    JIRAReportInfo jrInfo = null;
                    if (JIRAMatch.isEmpty()) {
                        log.severe("skipping JIRA report not found: " + teamName + ", sprint " + sprint);
                    } else {
                        jrInfo = JIRAMatch.get();
                    }
                    if (spreportInfo == null
                            && jrInfo == null
                            && capacityInfo == null) {
                        continue;
                    }
                    row = sheet.createRow(rownum++);
                    row.createCell(cellnum++, CellType.STRING).setCellValue(teamName);
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(year);
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(getQuarter(sprint));
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(sprint);
                    if (spreportInfo != null) {
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getEstimated());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getAllComplete());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getRemoved());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getAddedAst());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getSpVar());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(spreportInfo.getCompRatio());
                    } else {
                        cellnum += 6;
                    }
                    if (jrInfo != null) {
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getTotalTickets());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getCompletedTickets());
                    } else {
                        cellnum += 2;
                    }
                    if (capacityInfo != null) {
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getEffectiveHours());
                    } else {
                        cellnum++;
                    }
                    if (jrInfo != null) {
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getToDo());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getInProgress());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getInReview());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(jrInfo.getOnHold());
                    } else {
                        cellnum += 4;
                    }
                    if (capacityInfo != null) {
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getDevelopers());
                        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getCoefficient());
                    }
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return sheet;
    }

    private static XSSFCellStyle buildHeaderStyle(IndexedColors color) {
        StylesTable stylesTable = new StylesTable();
        XSSFCellStyle cellStyle = new XSSFCellStyle(stylesTable);
        XSSFFont font = new XSSFFont();
        font.setBold(true);
        cellStyle.setFont(font);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillBackgroundColor(color.getIndex());
        return cellStyle;
    }

    private static int getYear() {
        return Integer.parseInt(sysProps.getProperty("year"));
    }

    private static int getQuarter(Integer sprint) {
        return sprintxquarter.get(sprint);
    }

    private static void fillProfileInfo() {
        int quarter = 1;
        int nextQrSprint = Integer.parseInt(sysProps.getProperty("quarter." + (quarter + 1) + ".sprint", "-1"));
        for (int sprint = 1; sprint <= 26; sprint++) {
            if (sprint == nextQrSprint) {
                quarter++;
                nextQrSprint = Integer.parseInt(sysProps.getProperty("quarter." + (quarter + 1) + ".sprint", "-1"));
            }
            sprintxquarter.put(sprint, quarter);
//            log.log(Level.INFO, "sprint:{0}, quarter:{1}", new Object[]{sprint, quarter});
        }
        for (quarter = 1; quarter <= 4; quarter++) {
            String bbUrl = sysProps.getProperty("bug bash.q" + quarter);
            quarterQC.put(quarter, bbUrl);
        }
    }

    public static Workbook QCbuildWB(String destPath, List<String> teamNames, List<Integer> quarters, SwingWorker worker) throws FileNotFoundException, IOException, InvalidVarNameException, InvalidParamException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        FileOutputStream out = new FileOutputStream(destPath);
        QCbuildSheet(workbook, teamNames, quarters, worker);
        workbook.write(out);
        workbook.close();
        out.close();
        return workbook;
    }

    private static Sheet QCbuildSheet(Workbook wb, List<String> teamNames, List<Integer> quarters, SwingWorker worker) throws IOException, InvalidVarNameException, InvalidParamException {
        Sheet resp = wb.createSheet("Quality Check");
        List<Ticket> reportedBugs = new LinkedList<>();
        List reportedTestCases = null;
        int rownum = 0, cellnum = 0;
        CreationHelper creationHelper = wb.getCreationHelper();
        reportedBugs.addAll(QCReportedBugsData(resp, quarters, teamNames, creationHelper, worker));
        worker.firePropertyChange("progress", 95, 100);
        return resp;
    }

    private static void QCGetRACases(List<String> epics, SwingWorker worker) throws IOException, InvalidVarNameException, InvalidParamException {
        AntSMUtilites.run("go={[:CONFLUENCE_TPP_ROOT]}");
    }

    private static List<Ticket> QCReportedBugsData(Sheet sheet, List<Integer> quarters, List<String> teamNames, CreationHelper creationHelper, SwingWorker worker) throws IOException, InvalidVarNameException, InvalidParamException {
//        log.info("Teams: "+teamNames.stream().collect(joining(",")));
//        log.info("Quarters: "+quarters.stream().map(Object::toString).collect(joining(",")));
        String JIRA_HOME = AntSMUtilites.parse("[:JIRA_HOME]");
        final WebDriver driver = AntSMUtilites.getDriver();
        int rownum = 0, cellnum = 0;
        List<Ticket> reportedBugs = new LinkedList<>();
        Row row = sheet.createRow(rownum++);
        XSSFCellStyle style = buildHeaderStyle(IndexedColors.GREEN);
        Cell cell = row.createCell(cellnum);
        cell.setCellValue("Reported Bugs in Bug bash");
        row.setRowStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rownum - 1, rownum - 1, 0, 7));
        row = sheet.createRow(rownum++);
        //headers
        for (String header
                : new String[]{"Team",
                    "Quarter", "Key", "Epic", "Type", "Asignee", "Fix Engineers", "Status"}) {
            cell = row.createCell(cellnum++);
            IndexedColors color = IndexedColors.BRIGHT_GREEN;
            cell.setCellValue(header);
            XSSFCellStyle cellStyle = buildHeaderStyle(color);
            cell.setCellStyle(cellStyle);
        }

        row.setRowStyle(style);
        sheet.createFreezePane(1, 2);

        int delta = 80 / (quarters.size() * teamNames.size());
//        log.info("delta: "+delta);
        double total = 5d;
        for (Integer quarter : quarters) {
            String url = quarterQC.get(quarter);
            String selector = "li.menu-item > a.tab-nav-link";
            AntSMUtilites.run("go={" + url + "}\n"
                    + "pause={\"time\":\"[:longdelay]\"}\n"
                    + "wait={\"selector\":\"" + selector + "\"}");
            List<WebElement> headers = driver.findElements(By.cssSelector(selector));
            int headersCount = headers.size();
            if (headersCount == 0) {
                log.severe("No info found for quarter " + quarter + " in " + url);
            }
            double minidelta = 15d / headersCount;
            for (int i = 0; i < headersCount; i++) {
                worker.firePropertyChange("progress", new Double(total - minidelta).intValue(), new Double(total).intValue());
                total += minidelta;
                selector = "li.menu-item:nth-of-type(" + (i + 1) + ") > a.tab-nav-link";
                AntSMUtilites.run("wait={\"selector\":\"" + selector + "\"}");
                WebElement header = driver.findElement(By.cssSelector(selector));
                header.click();
                String title = header.getText();
                log.log(Level.INFO, "reading header {0} {1} of {2}", new Object[]{title, (i + 1), headersCount});
                AntSMUtilites.run("pause={\"time\":\"[:longdelay]\"}");
                List<WebElement> tables = driver.findElements(By.cssSelector("table.aui"));
                if (tables.size() <= i) {
                    break;
                }
                WebElement table = tables.get(i);
                reportedBugs.addAll(collectBugBashes(title, table));
                AntSMUtilites.run("go={" + url + "}\n"
                        + "pause={\"time\":\"[:longdelay]\"}");
            }

            total = 15;

            for (String teamName : teamNames) {
                worker.firePropertyChange("progress", new Double(total - delta).intValue(), new Double(total).intValue());
                total += delta;
                List<Ticket> teamTickets = reportedBugs.stream()
                        .filter(t -> t.matchesTeam(teamName))
                        .collect(toList());
                for (Ticket teamTicket : teamTickets) {
                    cellnum = 0;
                    row = sheet.createRow(rownum++);
                    row.createCell(cellnum++).setCellValue(teamName);
                    row.createCell(cellnum++).setCellValue(quarter);
                    //Key
                    cell = row.createCell(cellnum++);
                    Hyperlink link = creationHelper.createHyperlink(HyperlinkType.URL);
                    link.setAddress(teamTicket.getURL());
                    cell.setHyperlink(link);
                    cell.setCellValue(teamTicket.getKey());
                    //Epic
                    cell = row.createCell(cellnum++);
                    final String epic = teamTicket.getEpic();
                    if (epic != null) {
                        link = creationHelper.createHyperlink(HyperlinkType.URL);
                        link.setAddress(JIRA_HOME + "/browse/" + epic);
                        cell.setHyperlink(link);
                        cell.setCellValue(epic);
                    }

                    row.createCell(cellnum++).setCellValue(teamTicket.getType().name());
                    row.createCell(cellnum++).setCellValue(teamTicket.getAssignee());
                    row.createCell(cellnum++).setCellValue(teamTicket.getFixEngineers().stream().collect(joining(",")));
                    row.createCell(cellnum++).setCellValue(teamTicket.getStatus());
                }
            }
        }
        return reportedBugs;
    }

}
