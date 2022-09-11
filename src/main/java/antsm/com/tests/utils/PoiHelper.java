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
import antsm.com.tests.logic.SPreportDimension;
import antsm.com.tests.logic.SPreportDimension.Dimension;
import antsm.com.tests.logic.Ticket;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import antsm.com.tests.swing.ControlPanel.Action;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacities;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacitiesCache;
import static antsm.com.tests.utils.ConfluenceHelper.inCapacitiesCache;
import static antsm.com.tests.utils.JIRAReportHelper.collectBugBashes;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAName;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRARCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAReports;
import static antsm.com.tests.utils.JIRAReportHelper.guessTicketName;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

    private enum ReportMode {
        FORM1,// Team	Year	Quarter	Sprint	Estimated	Completed	Removed	Added	Story Points Variance	Completion Ratio	Total Tickets	Completed Tickets	Effective hours	To Do	In Progress	In Review	On hold	Developers per sprint	Team Coefficient
        FORM2//"Team", "Year", "Quarter", "Sprint", col1, [col2,] "Estimated", "Completed",
        //"SP variance", "Utilization Ratio", "Completion Ratio

    }

    public static void init() {
        sysProps = getConfigFile();
        fillProfileInfo();
        year = Integer.parseInt(sysProps.getProperty("year", "0"));
//        log.info("year:" + year);
    }

    public static void destroy() {
        sprintxquarter.clear();
    }

    public static Workbook buildStatsWB(String destPath, List<String> teamNames, List<Integer> sprints,
            List<Action> actions, SwingWorker worker)
            throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException, Exception {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        double factor = 1 / actions.size();
        int progress = Double.valueOf(factor).intValue();
        try {
            for (Action action : actions) {
                switch (action) {
                    case DATA_BASE:
                        buildDatabase(workbook, teamNames, sprints, factor, worker);
                        break;
                    case DP_REPORT:
                        buildDPMetrics(workbook, teamNames, sprints, factor, worker);
                        break;
                    case EPIC_REPORT:
                        buildEpicMetrics(workbook, teamNames, sprints, factor, worker);
                        break;
                    case PERSONAL_COEFFICIENT:
                        buildPersonalMetrics(workbook, teamNames, sprints, factor, worker);
                        break;
                }
                progress += (factor * 100);
                worker.firePropertyChange("progress", worker.getProgress(), progress);
            }
        } finally {
            FileOutputStream out = new FileOutputStream(destPath);
            log.info("writing output to " + destPath);
            workbook.write(out);
            workbook.close();
            out.close();
            worker.firePropertyChange("progress", 95, 100);
        }
        return workbook;
    }

    private static Sheet buildDPMetrics(Workbook wb, List<String> teamNames, List<Integer> sprints, double factor, SwingWorker worker)
            throws InvalidParamException, OpenXML4JException, InvalidVarNameException, InvalidVarNameException, Exception {
        Sheet sheet = wb.createSheet("Metrics per DP");
        buildMetricsByDimension(sheet, worker, factor, teamNames, sprints, SPreportDimension.Dimension.DP);
        return sheet;
    }

    private static Sheet buildEpicMetrics(Workbook wb, List<String> teamNames, List<Integer> sprints, double factor, SwingWorker worker)
            throws InvalidParamException, OpenXML4JException, InvalidVarNameException, InvalidVarNameException, Exception {
        Sheet sheet = wb.createSheet("Metrics per Epic");
        buildMetricsByDimension(sheet, worker, factor, teamNames, sprints, SPreportDimension.Dimension.EPIC);
        return sheet;
    }

    private static Sheet buildPersonalMetrics(Workbook wb, List<String> teamNames, List<Integer> sprints, double factor, SwingWorker worker)
            throws InvalidParamException, OpenXML4JException, InvalidVarNameException, InvalidVarNameException, Exception {
        Sheet sheet = wb.createSheet("Personal Metrics");
        buildMetricsByDimension(sheet, worker, factor, teamNames, sprints, SPreportDimension.Dimension.INDIVIDUAL);
        return sheet;
    }

    private static void buildMetricsByDimension(Sheet sheet, SwingWorker worker, double factor, List<String> teamNames, List<Integer> sprints, SPreportDimension.Dimension dimension) throws Exception {
        int rownum = 0, cellnum = 0;
        Row row = sheet.createRow(rownum++);
        //headers
        String col1 = null, col2 = null;
        switch (dimension) {
            case DP:
                col1 = "DP";
                col2 = "DP Name";
                break;
            case EPIC:
                col1 = "Epic";
                col2 = "Epic Name";
                break;
            case INDIVIDUAL:
                col1 = "Name";
                col2 = "Personal Coefficient";
                break;
        }
        String[] headers = null;
        switch (dimension) {
            case DP:
            case EPIC:
                headers = new String[]{"Team", "Year", "Quarter", "Sprint", col1, col2, "Estimated", "Completed",
                    "SP variance", "Utilization Ratio", "Completion Ratio"};
                break;
            case INDIVIDUAL:
                headers = new String[]{"Team", "Year", "Quarter", "Sprint", col1, "Estimated", "Completed",
                    "SP variance", "Utilization Ratio", "Completion Ratio", col2};
        }
        for (String header : headers) {
            Cell cell = row.createCell(cellnum++);
            IndexedColors color = IndexedColors.BRIGHT_GREEN;
            cell.setCellValue(header);
            XSSFCellStyle cellStyle = buildHeaderStyle(color);
            cell.setCellStyle(cellStyle);
        }
        sheet.createFreezePane(1, 1);
        int total = worker.getProgress();
        worker.firePropertyChange("progress", 0, total + Double.valueOf(5 * factor).intValue());
        int delta = 90 / teamNames.size();
        //            log.info("delta:"+delta);
        total += (5 * factor);
        for (String teamName : teamNames) {
            worker.firePropertyChange("progress", total - delta, total + Double.valueOf(total).intValue());
            total += delta;
            String JIRAid = getJIRAName(teamName);
            if (JIRAid == null) {
                log.log(Level.SEVERE, "couldn''t find JIRA id for team {0}", teamName);
                continue;
            }
            final List<CapacityInfo> capacities
                    = inCapacitiesCache(teamName, sprints)
                    ? getCapacitiesCache(teamName, sprints)
                    : getCapacities(teamName, sprints);
            final List<SPReportInfo> spreports
                    = inSPRCache(teamName, sprints)
                    ? getSPRCache(teamName, sprints)
                    : getSPReports(teamName, JIRAid, sprints);
            final List<JIRAReportInfo> JIRAReports
                    = inJIRARCache(teamName, sprints)
                    ? getJIRARCache(teamName, sprints)
                    : getJIRAReports(teamName, JIRAid, sprints);

            for (Integer sprint : sprints) {
                Predicate<AbstractTeamSprintInfo> teamSprintSearcher = (x -> x.getTeamName().equals(teamName) && x.getSprint() == sprint);
                Optional<CapacityInfo> capacityMatch = capacities.stream().filter(teamSprintSearcher).findFirst();
                CapacityInfo capacityInfo = null;
                if (capacityMatch.isEmpty()) {
                    log.log(Level.SEVERE, "skipping capacity not found: {0}, sprint {1}", new Object[]{teamName, sprint});
                } else {
                    capacityInfo = capacityMatch.get();
                }
//                log.info(spreports.stream().map(SPReportInfo::shortPrint).collect(joining("\n")));
                Optional<SPReportInfo> SPRMatch = spreports.stream().filter(teamSprintSearcher).findFirst();
                SPReportInfo spreportInfo = null;
                if (SPRMatch.isEmpty()) {
                    log.log(Level.SEVERE, "skipping python report not found: {0}, sprint {1}", new Object[]{teamName, sprint});
                } else {
//                    log.info("found something");
                    spreportInfo = SPRMatch.get();
                }
                Optional<JIRAReportInfo> JIRAMatch = JIRAReports.stream().filter(teamSprintSearcher).findFirst();
//                log.info(JIRAReports.stream().map(JIRAReportInfo::shortPrint).collect(joining("\n")));
                JIRAReportInfo jrInfo = null;
                if (JIRAMatch.isEmpty()) {
                    log.log(Level.SEVERE, "skipping JIRA report not found: {0}, sprint {1}", new Object[]{teamName, sprint});
                } else {
                    jrInfo = JIRAMatch.get();
                }
                if (spreportInfo == null
                        && jrInfo == null
                        && capacityInfo == null) {
                    continue;
                }
                if (spreportInfo != null && jrInfo != null) {
                    List<SPreportDimension> dpreports = spreportInfo.getMany(dimension);
                    for (SPreportDimension dp : dpreports) {
                        row = sheet.createRow(rownum++);
                        fillXLRow(spreportInfo, jrInfo, dp, teamName, sprint, ReportMode.FORM2, capacityInfo, row);
                    }
                }
            }
        }
    }

    private static Sheet buildDatabase(Workbook wb, List<String> teamNames, List<Integer> sprints, double factor, SwingWorker worker)
            throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        int rownum = 0, cellnum = 0;
        Sheet sheet = wb.createSheet("Data Base");
        Row row = sheet.createRow(rownum++);
        //headers
        for (String header
                : new String[]{"Team",
                    "Year", "Quarter", "Sprint", "Estimated", "Completed", "Removed", "Added", "Story Points Variance",
                    "Completion Ratio", "Total Tickets", "Completed Tickets", "Effective hours", "To Do", "In Progress", "In Review",
                    "On hold", "Developers per sprint", "Team Coefficient", "Real Value"}) {
            Cell cell = row.createCell(cellnum++);
            IndexedColors color = IndexedColors.BRIGHT_GREEN;
            cell.setCellValue(header);
            XSSFCellStyle cellStyle = buildHeaderStyle(color);
            cell.setCellStyle(cellStyle);
        }

        sheet.createFreezePane(1, 1);
        try {
            int total = worker.getProgress();
            worker.firePropertyChange("progress", 0, total + Double.valueOf(5 * factor).intValue());
            int delta = 90 / teamNames.size();
//            log.info("delta:"+delta);
            total += (5 * factor);
            for (String teamName : teamNames) {
                worker.firePropertyChange("progress", total - delta, total + Double.valueOf(total).intValue());
                total += delta;
                //Bajemos el capacity...
                final List<CapacityInfo> capacities
                        = inCapacitiesCache(teamName, sprints)
                        ? getCapacitiesCache(teamName, sprints)
                        : getCapacities(teamName, sprints);
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

                final List<JIRAReportInfo> JIRAReports
                        = inJIRARCache(teamName, sprints)
                        ? getJIRARCache(teamName, sprints)
                        : getJIRAReports(teamName, JIRAid, sprints);

                for (Integer sprint : sprints) {
                    Predicate<AbstractTeamSprintInfo> teamSprintSearcher = (x -> x.getTeamName().equals(teamName) && x.getSprint() == sprint);
                    Optional<CapacityInfo> capacityMatch = capacities.stream().filter(teamSprintSearcher).findFirst();
//                    log.info(capacities.stream().map(CapacityInfo::shortPrint).collect(joining("\n")));
                    CapacityInfo capacityInfo = null;
                    if (capacityMatch.isEmpty()) {
                        log.severe("skipping capacity not found: " + teamName + ", sprint " + sprint);
                    } else {
                        capacityInfo = capacityMatch.get();
//                        log.info(capacityMatch.toString());
                    }
                    Optional<SPReportInfo> SPRMatch = spreports.stream().filter(teamSprintSearcher).findFirst();
                    SPReportInfo spreportInfo = null;
                    if (SPRMatch.isEmpty()) {
                        log.severe("skipping python report not found: " + teamName + ", sprint " + sprint);
                    } else {
                        spreportInfo = SPRMatch.get();
                    }
                    SPreportDimension spgroup = spreportInfo.get(SPreportDimension.Dimension.GROUP);
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
                    if (spgroup != null) {
                        row = sheet.createRow(rownum++);
                        fillXLRow(spreportInfo, jrInfo, spgroup, teamName, sprint, ReportMode.FORM1, capacityInfo, row);
                    }
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return sheet;
    }

    private static void fillXLRow(SPReportInfo reportInfo, JIRAReportInfo iRAReportInfo, SPreportDimension dimReport,
            String teamName, int sprint, ReportMode mode, CapacityInfo capacityInfo, Row row) {
        fillXLRow(reportInfo, iRAReportInfo, dimReport, teamName, sprint, mode, capacityInfo, row, 0);
    }

    private static void fillXLRow(SPReportInfo reportInfo, JIRAReportInfo iRAReportInfo, SPreportDimension dimReport,
            String teamName, int sprint, ReportMode mode, CapacityInfo capacityInfo, Row row, int cellnum) {
        row.createCell(cellnum++, CellType.STRING).setCellValue(teamName);
        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(year);
        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(getQuarter(sprint));
        row.createCell(cellnum++, CellType.NUMERIC).setCellValue(sprint);
        switch (dimReport.getDimension()) {
            case DP://DP,DP Name
            case EPIC://Epic,Epic Name
                Cell cell = row.createCell(cellnum++, CellType.STRING);
                cell.setCellValue(dimReport.getId());
                 {
                    try {
                        row.createCell(cellnum, CellType.STRING).setCellValue(guessTicketName(dimReport.getId()));
                        //Only if there's a valid epic/dp, link is created
                        CreationHelper creationHelper = row.getSheet().getWorkbook().getCreationHelper();
                        Hyperlink link = creationHelper.createHyperlink(HyperlinkType.URL);
                        link.setAddress("https://jira.bbpd.io/browse/" + dimReport.getId());
                        cell.setHyperlink(link);
                    } catch (Exception ex) {
                    } finally {
                        cellnum++;
                    }
                }
                break;
            case INDIVIDUAL:
                row.createCell(cellnum++, CellType.STRING).setCellValue(dimReport.getUserName());
                break;
        }

        if (reportInfo != null) {
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getEstimated());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getAllComplete());
            switch (mode) {
                case FORM1:
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getRemoved());
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getAddedAst());
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getSpVar());
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getCompRatio());
                    break;
                case FORM2:
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getSpVar());
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getUtRatio());
                    row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getCompRatio());
                    if (dimReport.getDimension().equals(Dimension.INDIVIDUAL)) {
                        String userName = dimReport.getUserName();
//                        log.log(Level.INFO, "buscando coeficiente de {0}", userName);
                        if (capacityInfo == null) {
                            log.severe("Capacity not found : " + teamName + "," + sprint);
                            break;
                        }
                        Double coefficient = capacityInfo.getCoefficient(userName);
                        if (coefficient == null) {
                            String alias = getAliasInCapacity(teamName, userName);
                            coefficient = capacityInfo.getCoefficient(alias);
                        }

                        if (coefficient != null) {
                            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(coefficient);
                        } else {
                            String teamMembers = capacityInfo.getPersonalCoefficients().keySet().stream().collect(joining(","));
                            log.log(Level.WARNING, "{0}: couldn''t find {1} among users in capacity :{2}", new Object[]{capacityInfo.shortPrint(), userName, teamMembers});
                        }
                    }
                    return;
            }
        } else {
            cellnum += 6;
            switch (mode) {
                case FORM2:
                    return;
            }
        }
        if (iRAReportInfo != null) {
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getTotalTickets());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getCompletedTickets());
        } else {
            cellnum += 2;
        }
        if (capacityInfo != null) {
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getEffectiveHours());
        } else {
            cellnum++;
        }
        if (iRAReportInfo != null) {
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getToDo());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getInProgress());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getInReview());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(iRAReportInfo.getOnHold());
        } else {
            cellnum += 4;
        }
        if (capacityInfo != null) {
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getDevelopers());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(capacityInfo.getCoefficient());
        } else {
            cellnum += 2;
        }
        if (reportInfo != null) {
//                        log.info(spreportInfo.getTeamName()+","+spreportInfo.getSprint()+", r.v:"+spreportInfo.getRealValue());
            row.createCell(cellnum++, CellType.NUMERIC).setCellValue(dimReport.getRealValue());
        }
    }

    private static String getAliasInCapacity(String teamName, String userName) {
        String alias = "";
        String capacityMapping = sysProps.getProperty("mapping." + teamName + ".capacity");
        if (capacityMapping != null) {
            JSONParser parser = new JSONParser();
            try {
                JSONObject JSONMapping = (JSONObject) parser.parse(capacityMapping);
                alias = (String) JSONMapping.get(userName);
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
        return alias;
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

    public static Workbook QCbuildWB(String destPath, List<String> teamNames, List<Integer> quarters, SwingWorker worker)
            throws FileNotFoundException, IOException, InvalidVarNameException, InvalidParamException, Exception {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        FileOutputStream out = new FileOutputStream(destPath);
        try {
            QCbuildSheet(workbook, teamNames, quarters, worker);
        } finally {
            workbook.write(out);
            workbook.close();
            out.close();
        }
        return workbook;
    }

    private static Sheet QCbuildSheet(Workbook wb, List<String> teamNames, List<Integer> quarters, SwingWorker worker)
            throws IOException, InvalidVarNameException, InvalidParamException, Exception {
        Sheet resp = wb.createSheet("Quality Check");
        List<Ticket> reportedBugs = new LinkedList<>();
        List reportedTestCases = null;
        int rownum = 0, cellnum = 0;
        CreationHelper creationHelper = wb.getCreationHelper();
        reportedBugs.addAll(QCReportedBugsData(resp, quarters, teamNames, creationHelper, worker));
        worker.firePropertyChange("progress", 95, 100);
        return resp;
    }

    private static void QCGetRACases(List<String> epics, SwingWorker worker) throws IOException, InvalidVarNameException, InvalidParamException, Exception {
        AntSMUtilites.run("go={[:CONFLUENCE_TPP_ROOT]}");
    }

    private static List<Ticket> QCReportedBugsData(Sheet sheet, List<Integer> quarters, List<String> teamNames, CreationHelper creationHelper, SwingWorker worker)
            throws IOException, InvalidVarNameException, InvalidParamException, Exception {
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
                worker.firePropertyChange("progress", Double.valueOf(total - minidelta).intValue(), Double.valueOf(total).intValue());
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
                worker.firePropertyChange("progress", Double.valueOf(total - delta).intValue(), Double.valueOf(total).intValue());
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
