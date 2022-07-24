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
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacityCache;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraPassword;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraUser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    private static final String spreportPath;
    private static final String pythonPath;

    static {
        sysProps = getConfigFile();
        spreportPath = sysProps.getProperty("JIRA.spreportPath");
        pythonPath = sysProps.getProperty("JIRA.pythonPath");
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
    public static List<SPReportInfo> getSPReports(String teamName, String JIRAid, List<Integer> sprints) throws IOException, InvalidParamException, InvalidVarNameException, OpenXML4JException {
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
            log.log(Level.INFO, "running command [{0}]", printCommand.replace("{sprint}", "" + sprint));
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command, null, new File(spreportPath));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

// Read the output from the command
//            log.info("Here is the standard output of the command:");
            String s = null;
            int linecounter = 1;
            int baseOffset = 0;
            while ((s = stdInput.readLine()) != null) {
//                log.info(s);
                //titles
                if (linecounter == 2) {
                    baseOffset = s.indexOf("Estimated");
                }
                //Overall
                if (linecounter == 3) {
//                    log.info("filling spinfo");
//                    log.log(Level.INFO, "taking {0}", s.substring(baseOffset));
                    final String[] tokens = s.substring(baseOffset).trim().split("\\s+");

                    int field = 0;
                    spinfo.setEstimated(Double.parseDouble(tokens[field++]));
                    spinfo.setAllComplete(Double.parseDouble(tokens[field++]));
                    spinfo.setIncomplete(Double.parseDouble(tokens[field++]));
                    spinfo.setRemoved(Double.parseDouble(tokens[field++]));
                    spinfo.setAddedAst(Double.parseDouble(tokens[field++]));
                    spinfo.setCompleteAst(Double.parseDouble(tokens[field++]));
                    spinfo.setIncompleteAst(Double.parseDouble(tokens[field++]));
                    spinfo.setComplete(Double.parseDouble(tokens[field++]));
                    spinfo.setAllIncomplete(Double.parseDouble(tokens[field++]));
                    spinfo.setCias(Double.parseDouble(tokens[field++]));

                    final CapacityInfo capacity = getCapacityCache(teamName, sprint);
                    if (capacity == null) {
                        break;
                    }
                    spinfo.setIdeal(capacity.getIdeal());
                    resp.add(spinfo);
                    spreportCache.add(spinfo);
                    s = null;
                    break;
                }
                linecounter++;
            }

// Read any errors from the attempted command
//            log.info("Here is the standard error of the command (if any):\n");
//            while ((s = stdError.readLine()) != null) {
//                log.info(s);
//            }
        }
        return resp;
    }

    public static boolean inSPRCache(String teamName, Integer sprint) {
        SPReportInfo sample = new SPReportInfo(teamName, sprint);
        return spreportCache.contains(sample);
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
            SPReportInfo sample = new SPReportInfo(teamName, sprint);
            final Optional<SPReportInfo> spMatch = spreportCache.stream()
                    .filter(sp -> sp.equals(sample))
                    .findFirst();
            resp.add(spMatch.get());
        }
        return resp;
    }

    public static boolean inSPRCache(String teamName, List<Integer> sprints) {
        for (Integer sprint : sprints) {
            if (!inSPRCache(teamName, sprint)) {
                return false;
            }
        }
        return true;
    }

    public static String getSpreportPath() {
        return spreportPath;
    }

    public static String getPythonPath() {
        return pythonPath;
    }

}
