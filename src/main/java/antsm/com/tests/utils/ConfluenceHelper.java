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

import antsm.com.tests.logic.CapacityInfo;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.plugins.AntSMUtilites.runTemplate;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author nesto
 */
public final class ConfluenceHelper {

    private static Properties sysProps;
    private static Logger log = Logger.getLogger("WebAppTester");
    private static List<CapacityInfo> capacityCache = new LinkedList<>();

    public static void init() {
        sysProps = getConfigFile();
        try {
            String home = sysProps.getProperty("CONFLUENCE.home");
//            log.info("setting confluence home to "+home);
            String command = "set={\"name\":\"CONFLUENCE_HOME\",\"value\":\""+home+"\"}";
//            log.info(command);
            AntSMUtilites.run(command);
//            log.info("done");
            home = sysProps.getProperty("CONFLUENCE.tpp.root");
            command = "set={\"name\":\"CONFLUENCE_TPP_ROOT\",\"value\":\""+home+"\"}";
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
    
    public static void destroy(){
        capacityCache.clear();
    }

    public static List<CapacityInfo> getCapacities(String teamName, List<Integer> sprints) throws IOException, InvalidVarNameException, InvalidParamException, InvalidFormatException, OpenXML4JException {
        List<CapacityInfo> resp = new LinkedList<>();
        runTemplate("downloadCapacity.txt", teamName);
        //busca el archivo más reciente descargado.
        final File downloadDir = new File(System.getProperty("user.home") + File.separatorChar + "downloads");
        Comparator<File> sortByDate = (a, b) -> {
            try {
                final BasicFileAttributes attsA = Files.readAttributes(a.toPath(), BasicFileAttributes.class);
                final BasicFileAttributes attsB = Files.readAttributes(b.toPath(), BasicFileAttributes.class);
                return attsB.creationTime().compareTo(attsA.creationTime());
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
                return 0;
            }
        };
//        log.info("reading " + downloadDir.getAbsolutePath() + " files");
        final File[] xlsxFiles = downloadDir.listFiles((dir, name) -> name.endsWith(".xlsx"));
//        if (xlsxFiles == null) {
//            log.info(xlsxFiles.toString());
//        } else {
//            Arrays.asList(xlsxFiles).stream().forEach(f -> log.info(f.getName()));
//        }
        final Optional<File> capacityFileOpt = Arrays
                .asList(xlsxFiles)
                .stream()
                .sorted(sortByDate)
                .findFirst();
        if (capacityFileOpt.isEmpty()) {
            throw new IOException("file not found!");
        }
        File capacity = capacityFileOpt.get();
//        log.info("reading"+capacity.getAbsolutePath());
//        capacity.deleteOnExit();
        //Read the XLXS file
        XSSFWorkbook wb = new XSSFWorkbook(capacity);
        for (Integer sprint : sprints) {
            final CapacityInfo sampleCapacity = new CapacityInfo(teamName, sprint);
            if (inCapacityCache(teamName, sprint)) {
                continue;
            }
            CapacityInfo cinfo = new CapacityInfo();
            cinfo.setSprint(sprint);
            cinfo.setTeamName(teamName);
            final String sheetName = "Sprint " + sprint;
            final XSSFSheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                log.log(Level.SEVERE, "couldn''t find the sheet \"{0}\" for the team {1} , sprint {2}", new Object[]{sheetName, teamName, sprint});
                continue;
            }
            int developers = 0;
            for (Row row : sheet) {
                if (row.getRowNum() < 1) {
                    continue;
                }
                final Cell titleCell = row.getCell(0);
                String title = titleCell.getStringCellValue();
//                log.info("title:"+title);
                if (title.equals("Agregates") || title.equals("Aggregates")) {
                    try {
                        //ideal
                        cinfo.setIdeal(row.getCell(5).getNumericCellValue());
//                    effectiveHours
                        cinfo.setEffectiveHours(row.getCell(7).getNumericCellValue());
                        //commited
                        cinfo.setCommited(row.getCell(8).getNumericCellValue());
                        cinfo.setCoefficient(row.getCell(11).getNumericCellValue());
                    }//: Cannot get a NUMERIC value from a ERROR cell
                    catch (IllegalStateException | NullPointerException ex) {
                        log.severe("Error in sheet " + sheetName + " some results won't appear");
                    }
                    cinfo.setDevelopers(developers - 1);
                    resp.add(cinfo);
                    capacityCache.add(cinfo);
                    break;
                }
                final CellStyle rowStyle = row.getRowStyle();
                if (rowStyle == null || !rowStyle.getHidden()) {
                    developers++;
                }
            }
        }
        wb.close();
        capacity.delete();
        return resp;
    }

    public static CapacityInfo getCapacityCache(String teamName, int sprint) {
        CapacityInfo sample = new CapacityInfo(teamName, sprint);
//        log.info(sample.toString());
//        log.info("vs:");
//        capacityCache.forEach(c->log.info(c.toString()));
        final Optional<CapacityInfo> cmatch = capacityCache.stream().filter(c -> c.equals(sample)).findFirst();
        if(cmatch.isEmpty())
            return null;
        return cmatch.get();
    }

    public static String getCapacityURL(String teamName) {
        return sysProps.getProperty("team." + teamName + ".capacity");
    }

    public static boolean inCapacitiesCache(String teamName, List<Integer> sprints) {
        for (Integer sprint : sprints) {
            if (!inCapacityCache(teamName, sprint)) {
                return false;
            }
        }
        return true;
    }

    public static boolean inCapacityCache(String teamName, Integer sprint) {
        CapacityInfo sample = new CapacityInfo(teamName, sprint);
        return capacityCache.contains(sample);
    }

    /**
     * Looking for a specific capacity based on team & sprint
     *
     * @param teamName
     * @param sprint
     * @return
     * @throws IOException
     * @throws InvalidParamException
     * @throws InvalidVarNameException
     * @throws OpenXML4JException
     */
    public static CapacityInfo getCapacity(String teamName, Integer sprint) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
        List<CapacityInfo> capMatch = getCapacities(teamName, Arrays.asList(new Integer[]{sprint}));
        return capMatch.get(0);
    }

    /**
     * PRE: el capaciy que buscamos està en la lista.
     *
     * @param teamName
     * @param sprints
     * @return
     */
    public static List<CapacityInfo> getCapacitiesCache(String teamName, List<Integer> sprints) {
        List<CapacityInfo> resp = new LinkedList<>();
        for (Integer sprint : sprints) {
//            if (cmatch.isPresent()) {
            resp.add(getCapacityCache(teamName, sprint));
//            }
        }
        return resp;
    }

    public static void login() throws InvalidVarNameException, InvalidParamException, IOException {
        runTemplate("loginConfluence.txt");
    }

    public static void logout() throws IOException, InvalidVarNameException, InvalidParamException {
        AntSMUtilites.run("go={[:CONFLUENCE_HOME]/logout.action}");
    }
}
