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
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacities;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacitiesCache;
import static antsm.com.tests.utils.ConfluenceHelper.inCapacitiesCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAName;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRARCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJIRAReports;
import static antsm.com.tests.utils.JIRAReportHelper.inJIRARCache;
import static antsm.com.tests.utils.PythonReportHelper.getSPRCache;
import static antsm.com.tests.utils.PythonReportHelper.getSPReports;
import static antsm.com.tests.utils.PythonReportHelper.inSPRCache;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 *
 * @author nesto
 */
public final class PoiHelper {

    private static Logger log = Logger.getLogger("WebAppTester");
    private static Properties sysProps;
    private static int year;
    private static HashMap<Integer, Integer> sprintxquarter = new HashMap<>();

    public static void init() {
        sysProps = getConfigFile();
        fillSpQuarters();
        year = Integer.parseInt(sysProps.getProperty("year", "0"));
//        log.info("year:" + year);
    }

    public static void destroy(){
        sprintxquarter.clear();
    }
    
    public static Workbook buildDatabaseWB(String destPath, List<String> teamNames, List<Integer> sprints) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        buildDatabase(workbook, teamNames, sprints);
        FileOutputStream out = new FileOutputStream(destPath);
        workbook.write(out);
        return workbook;
    }

    private static Sheet buildDatabase(Workbook wb, List<String> teamNames, List<Integer> sprints) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        int rownum = 0, cellnum = 0;
        Sheet sheet = wb.createSheet("Data Base");
        Row row = sheet.createRow(rownum++);
        //headers
        for (String header
                : new String[]{"Team",
                    "Year", "Quarter", "Sprint", "Estimated", "Completed", "Removed", "Added", "Story Points Variance",
                    "Completion Ratio", "Total Tickets", "Completed Tickets", "Effective hours", "To Do", "In Progress", "In Review", "On hold", "Developers per sprint", "Team Coefficient"}) {
            Cell cell = row.createCell(cellnum++);
            cell.setCellValue(header);
            StylesTable stylesTable = new StylesTable();
            XSSFCellStyle cellStyle = new XSSFCellStyle(stylesTable);
            XSSFFont font = new XSSFFont();
            font.setBold(true);
            cellStyle.setFont(font);
            cellStyle.setFillBackgroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
            cell.setCellStyle(cellStyle);
        }

        sheet.createFreezePane(1, 1);
        try {
            for (String teamName : teamNames) {
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
                    if(spreportInfo==null &&
                            jrInfo==null &&
                            capacityInfo==null)
                        continue;
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

    private static int getYear() {
        return Integer.parseInt(sysProps.getProperty("year"));
    }

    private static int getQuarter(Integer sprint) {
        return sprintxquarter.get(sprint);
    }

    private static void fillSpQuarters() {
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
    }
}
