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
import antsm.com.tests.logic.SPReportInfo;
import antsm.com.tests.logic.SPreportDimension;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacityCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraPassword;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraUser;
import static antsm.com.tests.utils.JIRAReportHelper.getSPReportsKANBAN;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;

/**
 * El especialista corriendo reporte de python y trayendo información.
 *
 * @author nesto
 */
public final class PythonReportHelper {

    private static Properties sysProps;
    private static Logger log = Logger.getLogger("WebAppTester");
    private static List<SPReportInfo> spreportCache = new LinkedList<>();
    private static String spreportPath;
    private static String pythonPath;

    public static void init() {
        sysProps = getConfigFile();
        spreportPath = sysProps.getProperty("PYTHON.spreportPath");
        pythonPath = sysProps.getProperty("PYTHON.pythonPath");
    }

    public static void destroy() {
        spreportCache.clear();
    }

    /**
     * Thanks to
     * https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program
     *
     * @param JIRAid
     * @param sprints
     * @return
     * @throws IOException
     */
    public static List<SPReportInfo> getSPReports(String teamName, String JIRAid, List<Integer> sprints) 
            throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException, Exception {
        List<SPReportInfo> resp  = null;
        switch(JIRAReportHelper.getBoardType(Integer.parseInt(JIRAid))){
            case SCRUM:
                resp = getSPReportsSCRUM(JIRAid, teamName, sprints);
                break;
            case KANBAN:
                resp = getSPReportsKANBAN(JIRAid, teamName, sprints);
                break;
        }
        return resp;
    }

    private static List<SPReportInfo> getSPReportsSCRUM(String JIRAid, String teamName, List<Integer> sprints) throws Exception, IOException, NumberFormatException {
        List<SPReportInfo> resp = new LinkedList<>();
        final String SLASH = System.getProperty("file.separator");
        String genCommand = pythonPath + " sprint-report{slash}sprint-report.py  --output CONSOLE --team {team} "
                + "--user {JIRA_USER} --password {JIRA_PWD} --sprint {sprint}";
        genCommand = genCommand.replace("{team}", JIRAid);
        while (genCommand.contains("{slash}")) {
            genCommand = genCommand.replace("{slash}", SLASH);
        }
        String printCommand = genCommand;
        final String pwd = AntSMUtilites.parse(getJiraPassword());
        genCommand = genCommand.replace("{JIRA_USER}", getJiraUser())
                .replace("{JIRA_PWD}", pwd);
        //        log.info("genCommand=" + printCommand + ",spreportPath=" + spreportPath + ",JIRAid=" + JIRAid);
        genCommand = genCommand.replace("{base}", spreportPath);
        for (Integer sprint : sprints) {
            if (inSPRCache(teamName, sprint)) {
                continue;
            }
            String command = genCommand
                    .replace("{sprint}", "" + sprint);
            SPReportInfo spinfo = new SPReportInfo();

            spinfo.setTeamName(teamName);
            spinfo.setSprint(sprint);
//            log.info("looking for " + teamName + " , " + sprint);
if (inSPRCache(teamName, sprint)) {
//                log.info("found");
spinfo = getSPRCache(teamName, sprint);
}
log.log(Level.INFO, "running command [{0}]", printCommand.replace("{sprint}", "" + sprint));
Runtime rt = Runtime.getRuntime();
Process proc = rt.exec(command, null, new File(spreportPath));
BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

// Read the output from the command
//            log.info("Here is the standard output of the command:");
String s = null;
int linecounter = 1;
int estimatedOffset = 0;
int descriptionOffset = 0;
String[] tokens;
while ((s = stdInput.readLine()) != null) {
//                log.info(s);
//titles
if (linecounter == 2) {
    estimatedOffset = s.indexOf("Estimated");
    descriptionOffset = s.indexOf("Description");
}
//Overall
if (linecounter == 3) {
    tokens = s.substring(estimatedOffset).trim().split("\\s+");
    SPreportDimension spgroup = collectGroupStats(tokens, teamName, sprint);
    if (spgroup == null) {
        break;
    }
    spinfo.add(spgroup);
} else if (s.equals("By DP")) {
    stdInput.readLine();//ID          Description                     Estimated  All Complete  Incomplete  Removed   Added*  Complete*  Incomplete*  Complete  All Incomplete  CiAS
    s = stdInput.readLine();
    while (!s.isBlank()) {
        collectDPEpcStats(SPreportDimension.Dimension.DP, s, estimatedOffset, teamName, sprint, spinfo);
        s = stdInput.readLine();
    }
} else if (s.equals("By Epic")) {
    stdInput.readLine();//ID          Description                     Estimated  All Complete  Incomplete  Removed   Added*  Complete*  Incomplete*  Complete  All Incomplete  CiAS
    s = stdInput.readLine();
    while (!s.isBlank()) {
        collectDPEpcStats(SPreportDimension.Dimension.EPIC, s, estimatedOffset, teamName, sprint, spinfo);
        s = stdInput.readLine();
    }
} else if (s.equals("By Contributor")) {
    stdInput.readLine();//ID          Description                     Estimated  All Complete  Incomplete  Removed   Added*  Complete*  Incomplete*  Complete  All Incomplete  CiAS
    s = stdInput.readLine();
    while (!s.isBlank()) {
        collectIndividualResults(s, descriptionOffset, estimatedOffset, teamName, sprint, spinfo);
        s = stdInput.readLine();
    }
}
linecounter++;
}
//            log.info("adding " + spinfo.getTeamName()+","+spinfo.getSprint());
resp.add(spinfo);
if (!inSPRCache(teamName, sprint)) {
//                log.info("not in cache");
spreportCache.add(spinfo);
}
        }
        return resp;
    }

    private static void collectIndividualResults(String line, int descriptionOffset, int estimatedOffset, String teamName, Integer sprint, SPReportInfo spinfo) throws NumberFormatException {
        String[] tokens;
        SPreportDimension individual = new SPreportDimension(SPreportDimension.Dimension.INDIVIDUAL);
        String name = line.substring(descriptionOffset, estimatedOffset).trim();
        tokens = line.substring(estimatedOffset).trim().split("\\s+");
        int field = 0;
//        log.log(Level.INFO, "name:{0}", name);
        individual.setUserName(name);
        if (feedSPreport(individual, tokens, field, teamName, sprint)) {
//            log.info(individual.toString());
            spinfo.add(individual);
        }
    }

    /**
     * Collect DP or EPiC STATS
     *
     * @param dimension
     * @param s
     * @param baseOffset
     * @param teamName
     * @param sprint
     * @param spinfo
     * @throws NumberFormatException
     */
    private static void collectDPEpcStats(SPreportDimension.Dimension dimension, String s, int baseOffset, String teamName, Integer sprint, SPReportInfo spinfo) throws NumberFormatException {
        String[] tokens;
        SPreportDimension dimrep = new SPreportDimension(dimension);
        tokens = s.trim().split("\\s+");
        String id = tokens[0];
//        log.log(Level.INFO, "id:{0}", id);
        tokens = s.substring(baseOffset).trim().split("\\s+");
        int field = 0;
        dimrep.setId(id);
        if (feedSPreport(dimrep, tokens, field, teamName, sprint)) {
//            log.info(dimrep.toString());
            spinfo.add(dimrep);
        }
    }

    public static boolean inSPRCache(String teamName, Integer sprint) {
        SPReportInfo sample = new SPReportInfo(teamName, sprint);
//        log.info(spreportCache.stream().map(SPReportInfo::toString).collect(joining("\n")));
        return spreportCache.stream().anyMatch(r->r.getTeamName().equals(teamName) && r.getSprint()==sprint);
    }

    /**
     * PRE: el reporte que se busca està en la lista.
     *
     * @param teamName
     * @param sprints
     * @return
     */
    public static List<SPReportInfo> getSPRCache(String teamName, List<Integer> sprints) {
        List<SPReportInfo> resp = new LinkedList<>();
        for (Integer sprint : sprints) {
            SPReportInfo report = getSPRCache(teamName, sprint);
            if (report != null) {
                resp.add(report);
            }
        }
        return resp;
    }

    public static SPReportInfo getSPRCache(String teamName, int sprint) {
        Optional<SPReportInfo> match = spreportCache.stream().filter(r->r.getTeamName().equals(teamName)&&r.getSprint()==sprint)
                .findFirst();
        if (match.isPresent()) {
            return match.get();
        }
        return null;
    }

    public static boolean inSPRCache(String teamName, List<Integer> sprints) {
        for (Integer sprint : sprints) {
            if (!inSPRCache(teamName, sprint)) {
                return false;
            }
        }
//        log.info("returns true");
        return true;
    }

    public static String getSpreportPath() {
        return spreportPath;
    }

    public static String getPythonPath() {
        return pythonPath;
    }

    private static SPreportDimension collectGroupStats(String[] tokens, String teamName, int sprint) {

        int field = 0;
        SPreportDimension resp = new SPreportDimension(SPreportDimension.Dimension.GROUP);
        if (!feedSPreport(resp, tokens, field, teamName, sprint)) {
            return null;
        }
        return resp;
    }

    private static boolean feedSPreport(SPreportDimension resp, String[] tokens, int field, String teamName, int sprint) throws NumberFormatException {
        resp.setEstimated(Double.parseDouble(tokens[field++]));
        resp.setAllComplete(Double.parseDouble(tokens[field++]));
        resp.setIncomplete(Double.parseDouble(tokens[field++]));
        resp.setRemoved(Double.parseDouble(tokens[field++]));
        resp.setAddedAst(Double.parseDouble(tokens[field++]));
        resp.setCompleteAst(Double.parseDouble(tokens[field++]));
        resp.setIncompleteAst(Double.parseDouble(tokens[field++]));
        resp.setComplete(Double.parseDouble(tokens[field++]));
        resp.setAllIncomplete(Double.parseDouble(tokens[field++]));
        resp.setCias(Double.parseDouble(tokens[field++]));
        final CapacityInfo capacity = getCapacityCache(teamName, sprint);
        if (capacity == null) {
            return false;
        }
        resp.setIdeal(capacity.getIdeal());
        return true;
    }

}
