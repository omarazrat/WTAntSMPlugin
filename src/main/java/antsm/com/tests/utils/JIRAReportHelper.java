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
import antsm.com.tests.logic.Range;
import antsm.com.tests.logic.SPReportInfo;
import antsm.com.tests.logic.SPreportDimension;
import antsm.com.tests.logic.Ticket;
import antsm.com.tests.plugins.AntSMUtilites;
import static antsm.com.tests.plugins.AntSMUtilites.getConfigFile;
import static antsm.com.tests.plugins.AntSMUtilites.runTemplate;
import static antsm.com.tests.utils.JIRAReportHelper.TABLE_TYPE.COMPLETED_OUTSIDE_SPRINT;
import static antsm.com.tests.utils.JIRAReportHelper.TABLE_TYPE.REMOVED_FROM_SPRINT;
import static antsm.com.tests.utils.Utils.JSONRequest;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import org.apache.commons.collections4.map.HashedMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
    private static List<Ticket> ticketCache = new LinkedList<>();
    private static String JIRA_USER, JIRA_PWD;
    /**
     * Cache para {@link #guessTicketName(java.lang.String) }
     */
    private static final Map<String, String> ticketNames = new HashedMap<>();

    private static String storyPointsField,
            assigneeField,
            statusField,
            issueTypeField,
            teamsType,
            fixEngField,
            summaryField;

    private static Map<Range<Date>, Integer> sprintRanges;

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
        public String getContent() {
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

        public static TABLE_TYPE byHeader(String header) {
            for (TABLE_TYPE type : values()) {
                if (type.getContent().equals(header)) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum BOARD_TYPE {
        SCRUM,
        KANBAN
    }

    public static void init() {
        try {
            sysProps = getConfigFile();
            JIRA_USER = sysProps.getProperty("JIRA.user");
            JIRA_PWD = sysProps.getProperty("JIRA.password");
            storyPointsField = sysProps.getProperty("JREST.storyPointsField");
            assigneeField = sysProps.getProperty("JREST.assigneeField");
            statusField = sysProps.getProperty("JREST.statusField");
            issueTypeField = sysProps.getProperty("JREST.issueTypeField");
            teamsType = sysProps.getProperty("JREST.teamsType");
            fixEngField = sysProps.getProperty("JREST.fixEngField");
            summaryField = sysProps.getProperty("JREST.summaryField");
            String home = sysProps.getProperty("JIRA_home");

            AntSMUtilites.fnSetVariable("JIRA_home", home);

            sprintRanges = new HashMap<>();
            int year = Integer.parseInt(sysProps.getProperty("year"));
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            for (int sprint = 1; sprint <= 26; sprint++) {
                final String key = "range." + year + ".sprint." + sprint;
//                log.info("reading "+key);
                final String flatRange = sysProps.getProperty(key);
                String[] splittedRange = flatRange.split(",");
                Range<Date> range = new Range<Date>();
                range.setStart(df.parse(splittedRange[0]));
                range.setEnd(df.parse(splittedRange[1]));
                sprintRanges.put(range, sprint);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public static void destroy() {
        jirarepCache.clear();
    }

    public static void login() throws InvalidVarNameException, IOException, InvalidParamException, Exception {
        runTemplate("loginJIRA.txt");
    }

    public static void logout() throws InvalidVarNameException, IOException, InvalidParamException, Exception {
        runTemplate("logoutJIRA.txt");
    }

    public static String guessTicketName(String ticketId) throws InvalidVarNameException, InvalidParamException, Exception {
        if (ticketNames.containsKey(ticketId)) {
            return ticketNames.get(ticketId);
        }
        AntSMUtilites.fnGo("[:JIRA_home]/browse/" + ticketId);
        AntSMUtilites.fnWait();
        WebDriver driver = AntSMUtilites.getDriver();
        By selector = By.cssSelector("#summary-val");
        String resp = "";

        if (!driver.findElements(selector).isEmpty()) {
            WebElement element = driver.findElement(selector);
            resp = element.getAttribute("textContent");
        }
        ticketNames.put(ticketId, resp);
        return resp;
    }

    public static List<JIRAReportInfo> getJIRAReports(String teamName, String JIRAid, List<Integer> sprints) throws IOException, InvalidVarNameException, InvalidParamException, Exception {
        final List<String> exitStatuses = getExitStatuses(JIRAid);
        List<JIRAReportInfo> resp = null;
        switch (getBoardType(Integer.parseInt(JIRAid))) {
            case KANBAN:
                resp = new LinkedList<>();
                List<Ticket> teamTickets = getBoardIssues(JIRAid);
                for (Integer sprint : sprints) {
                    List<Ticket> sprintTickets = teamTickets
                            .parallelStream().filter(ticket -> ticket.matchesSprint(sprint))
                            .collect(toList());
                    JIRAReportInfo info = new JIRAReportInfo(teamName, sprint);
                    info.setTotalTickets(sprintTickets.size());
                    long completedTickets
                            = sprintTickets.parallelStream().filter(ticket -> exitStatuses.contains(ticket.getStatus()))
                                    .count();
                    info.setCompletedTickets((int) completedTickets);
                    info.setTotalTickets(sprintTickets.size());
                    resp.add(info);
                }
                break;
            case SCRUM:
            default:
                resp = getJIRAScrumReports(JIRAid, sprints, teamName);
                break;
        }
        return resp;
    }

    private static List<JIRAReportInfo> getJIRAScrumReports(String JIRAid, List<Integer> sprints, String teamName) throws Exception {
        List<JIRAReportInfo> resp = new LinkedList<>();
        final String location = "[:JIRA_home]/secure/RapidBoard.jspa?rapidView={team}&view=reporting&chart=sprintRetrospective"
                .replace("{team}", JIRAid);
        AntSMUtilites.run("go={" + location + "}");
        AntSMUtilites.fnPause("[:longdelay]");
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
            Pattern sprintSelector = Pattern.compile(".*" + teamName + ".*" + sprint + "(\\..*)?$");
            Predicate<WebElement> optFilter = opt -> {
                String text = opt.getAttribute("textContent");
//                log.info("testing opt: " + text);
                Matcher matcher = sprintSelector.matcher(text);
                return matcher.matches();
            };
            final Optional<WebElement> match = select.getOptions().stream().filter(optFilter).findFirst();
            if (!match.isPresent()) {
                log.log(Level.SEVERE, "Couldn't find sprint " + sprint + " of team " + teamName + " in JIRA sprint report ");
                continue;
            }
            WebElement opt = match.get();
            AntSMUtilites.run("go={" + location + "&sprint=" + opt.getAttribute("value") + "}");

            AntSMUtilites.run("pause={\"time\":\"[:longdelay]\"}");
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
                    if (tableType == null) {
                        log.log(Level.SEVERE, "table not matching any type found with index {0}{1}!", new Object[]{i, 1});
                        continue;
                    }
                    if (tableType.equals(COMPLETED_OUTSIDE_SPRINT)) {
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

    public static boolean inTicketCache(String key) {
        return ticketCache.parallelStream().anyMatch(t -> t.getKey().equals(key));
    }

    public static boolean inJIRARCache(String teamName, Integer sprint) {
        JIRAReportInfo report = new JIRAReportInfo(teamName, sprint);
        return jirarepCache.contains(report);
    }

    public static BOARD_TYPE getBoardType(int boardId) throws InvalidVarNameException, InvalidParamException, Exception {
        final String url = "https://jira.bbpd.io/rest/agile/1.0/board/" + boardId;
        JSONObject jsonObj = Utils.JSONRequest(url);
        if (jsonObj.get("type").equals("scrum")) {
            return BOARD_TYPE.SCRUM;
        }
        return BOARD_TYPE.KANBAN;
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

    public static Ticket getTicketCache(String key) {
        final Optional<Ticket> ticketMatch = ticketCache.parallelStream().filter(t -> t.getKey().equals(key)).findFirst();
        if (ticketMatch.isPresent()) {
            return ticketMatch.get();
        }
        return null;
    }

    public static Ticket getTicket(String key) {
        return toTicket.apply(key);
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
            String targetStatus = "";
            switch (desiredStatus) {
                case IN_PROGRESS:
                    targetStatus = "In Progress";
                    break;
                case IN_REVIEW:
                    targetStatus = "In review";
                    break;
                case ON_HOLD:
                    targetStatus = "On Hold";
                    break;
                case TODO:
                    targetStatus = "TO DO";
            }
//            log.log(Level.INFO, "got: {0}, desired: {1}.", new Object[]{status, desiredStatus});
            if (!status.equals(targetStatus.toUpperCase())) {
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
//        log.info("return true");
        return true;
    }

    /**
     * Trae el id del equipo como aparece en el tablero scrum o kanban
     *
     * @param teamName
     * @return
     */
    public static String getJIRAid(String teamName) {
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

    public static List<Ticket> collectBugBashes(String title, WebElement table) throws IOException, InvalidVarNameException, InvalidParamException, Exception {
        //Clave 	Resumen 	T 	Creado 	Actualizado 	Fecha de entrega 	Asignado 	Informante 	P 	Estado 	descripciÃ³n
        List<Ticket> resp = new LinkedList<>();
        List<WebElement> rows = null;
        try {
            rows = table.findElements(By.tagName("tr"));
        }//org.openqa.selenium.StaleElementReferenceException: stale element reference: element is not attached to the page document
        catch (Exception e) {
            log.log(Level.SEVERE, "error reading table {0}", title);
            return resp;
        }
        log.log(Level.INFO, "{0} in table {1}", new Object[]{rows.size(), title});
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.isEmpty()) {
                continue;
            }
            int index = 0;
            Ticket ticket = new Ticket();
            WebElement cell = cells.get(index++);
            WebElement anchor = cell.findElement(By.tagName("a"));
            String url = anchor.getAttribute("href");
            String key = anchor.getText();
            ticket.setKey(key);
            ticket.setURL(url);
//            log.info("got:" + ticket.toString());
            resp.add(ticket);
        }
        //reads ticket by ticket
        for (Ticket ticket : resp) {
//            log.info("got:" + ticket.toString());
            AntSMUtilites.run("go={" + ticket.getURL() + "}\n"
                    + "pause={\"time\":\"[:longdelay]\"}");
            final WebDriver driver = AntSMUtilites.getDriver();
            WebElement typeSpan = driver.findElement(By.cssSelector("#type-val"));
            ticket.setType(Ticket.TYPE.valueOf(typeSpan.getText().toUpperCase().trim()));
            WebElement assigneeSpan = driver.findElement(By.cssSelector("#assignee-val"));
            ticket.setAssignee(assigneeSpan.getText());
            WebElement statusSpan = driver.findElement(By.cssSelector(".jira-issue-status-lozenge"));
            ticket.setStatus(statusSpan.getText().toUpperCase());
            String selector = ".aui-label";
            boolean hasField = !driver.findElements(By.cssSelector(selector)).isEmpty();
            if (hasField) {
                WebElement epicLSpan = driver.findElement(By.cssSelector(selector));
                String epicCode = epicLSpan.getAttribute("href");
                final String word = "/browse/";
                int idx = epicCode.indexOf(word);
                if (idx > -1) {
                    epicCode = epicCode.substring(idx + word.length());
                }
                ticket.setEpic(epicCode);
            }
            //fix engineers
            List<WebElement> fixerSpans = driver.findElements(By.cssSelector("span.tinylink > span"));
            List<String> fixers = new LinkedList<>();
            for (WebElement fixerSpan : fixerSpans) {
                fixers.add(fixerSpan.getText());
            }
            ticket.setFixEngineers(fixers);
            //team(s)
            List<WebElement> teamSpans = driver.findElements(By.cssSelector("#customfield_16166-field > span"));
            List<String> teams = new LinkedList<>();
            for (WebElement teamSpan : teamSpans) {
                teams.add(teamSpan.getText());
            }
            ticket.setTeams(teams);
        }
        return resp;
    }

    /**
     * Atención: No utilice esta función concurrentemente {@link java.util.List#parallelStream()
     * }
     * mientras no exista una función que cargue independientemente de la
     * ventana principal cada invocacion
     */
    public static Function<String, Ticket> toTicket = (key) -> {
        Ticket resp = null;
        if (inTicketCache(key)) {
            return getTicketCache(key);
        }
        try {
            final String URL = AntSMUtilites.parse("[:JIRA_home]") + "/rest/api/2/issue/" + key + "?fields=" + storyPointsField + "," + assigneeField + ","
                    + statusField + "," + issueTypeField + "," + teamsType + "," + fixEngField + "," + summaryField;
            JSONObject jsonRoot = JSONRequest(URL);
            JSONObject jsonFields = (JSONObject) jsonRoot.get("fields");

            resp = new Ticket();
            resp.setKey(key);
            try {
                //Scrum points
                double doubleValue = (double) jsonFields.get(storyPointsField);
                resp.setPoints(doubleValue);
            } catch (Exception e) {
//                log.log(Level.SEVERE, "", e);
            }
            //Assignee
            JSONObject jsonAssignee = (JSONObject) jsonFields.get(assigneeField);
            if (jsonAssignee != null) {
                resp.setAssignee((String) jsonAssignee.get("displayName"));
            }
            JSONObject jsonStatus = (JSONObject) jsonFields.get(statusField);
            if (jsonStatus != null) {
                resp.setStatus(((String) jsonStatus.get("name")).toUpperCase());
            }
            resp.setURL(AntSMUtilites.parse("[:JIRA_home]") + "/browse/" + key);
            //type
            JSONObject issueType = (JSONObject) jsonFields.get(issueTypeField);
            if (issueType != null) {
                String jsonType = (String) issueType.get("name");
                resp.setType(Ticket.TYPE.valueOf(jsonType.toUpperCase()));
            }
            //Team(s)
            JSONArray teams = (JSONArray) jsonFields.get(teamsType);
            if (teams != null) {
                List<String> teamsNames = (List<String>) teams
                        .stream()
                        .map(v -> {
                            JSONObject teamItem = (JSONObject) v;
                            return (String) teamItem.get("value");
                        }).collect(toList());
                resp.setTeams(teamsNames);
            }
            //Fix Engineers
            JSONArray fixEngs = (JSONArray) jsonFields.get(fixEngField);
            if (fixEngs != null) {
                List<String> fixEngineers = (List<String>) fixEngs
                        .stream().map(v -> {
                            JSONObject fixEngineer = (JSONObject) v;
                            return fixEngineer.get("displayName");
                        }).filter(fe->!fe.toString().isBlank())
                        .distinct()
                        .collect(toList());
                resp.setFixEngineers(fixEngineers);
            }
            //summary
            resp.setSummary((String) jsonFields.get(summaryField));
        } catch (Exception e) {
            log.log(Level.SEVERE, key, e);
        }
        ticketCache.add(resp);
        return resp;
    };

    public static List<SPReportInfo> getSPReportsKANBAN(String JIRAid, String teamName, List<Integer> sprints) throws InvalidVarNameException, InvalidParamException, Exception {
        List<SPReportInfo> resp = new LinkedList<>();
        List<Ticket> tickets = getBoardIssues(JIRAid);
        final List<String> exitStatuses = getExitStatuses(JIRAid);
//        log.info(tickets.stream().map(Ticket::toString).collect(joining(",")));
//        log.info("collectiong epics");
        double pointsxSprint = Double.valueOf(sysProps.getProperty("JIRA.kanban.pointspersprint"));
        String key = "JIRA.kanban." + JIRAid + ".pointspersprint";
        if (sysProps.containsKey(key)) {
            pointsxSprint = Double.valueOf(sysProps.getProperty(key));
        }
//        log.info(epics.stream().map(Ticket::toString).collect(joining(",")));
        for (Integer sprint : sprints) {
            SPReportInfo info = new SPReportInfo(teamName, sprint);
            final List<Ticket> sprintTickets = tickets.parallelStream()
                    .filter(t -> t.matchesSprint(sprint))
                    .collect(toList());
            List<Ticket> epics = collectEpics(JIRAid);
            List<SPreportDimension> spreportDims = new LinkedList<>();
            //Epics
            for (Ticket epic : epics) {
                SPreportDimension epReport = new SPreportDimension(SPreportDimension.Dimension.EPIC);
                epReport.setId(epic.getKey());
                final List<Ticket> epicTickets = sprintTickets.parallelStream()
                        .filter(ticket -> {
                            final String tck_epic = ticket.getEpic();
                            return tck_epic.equals(epic.getKey());
                        })
                        .collect(toList());
                double estimated = epicTickets.parallelStream()
                        .map(Ticket::getPoints)
                        .reduce(0d, Double::sum);
                epReport.setEstimated(estimated);
                double completed = epicTickets.parallelStream()
                        .filter(ticket -> exitStatuses.contains(ticket.getStatus()))
                        .map(Ticket::getPoints)
                        .reduce(0d, Double::sum);
                epReport.setAllComplete(completed);
                epReport.setComplete(completed);
                epReport.setIncomplete(estimated - completed);
                spreportDims.add(epReport);
            }
            List<String> developers = sprintTickets.parallelStream()
                    .flatMap((Ticket ticket) -> {
                        List<String> names = ticket.getFixEngineers();
                        names.add(ticket.getAssignee());
                        return names.stream();
                    })
                    .filter(name -> name != null)
                    .distinct()
                    .collect(toList());
            final List<SPreportDimension> personalDimensions = new LinkedList<>();
            for (String developer : developers) {
                SPreportDimension perReport = new SPreportDimension(SPreportDimension.Dimension.INDIVIDUAL);
                perReport.setUserName(developer);
                double estimated = Utils.sumPointsByDeveloper(sprintTickets, developer),
                        completed = Utils.sumCompletedPointsByDeveloper(sprintTickets, exitStatuses, developer,sprint);
                perReport.setComplete(completed);
                perReport.setAllIncomplete(estimated - completed);
                perReport.setEstimated(estimated);
                perReport.setIdeal(pointsxSprint);
                personalDimensions.add(perReport);
//                log.info(perReport.toString());
            }
            spreportDims.addAll(personalDimensions);
            List<Ticket> completedTickets = sprintTickets.parallelStream().filter(ticket->Utils.closedInSprint(ticket, exitStatuses, sprint)).collect(toList());
            log.log(Level.INFO, "closed tickets 4 sp {0}:{1}", new Object[]{sprint, completedTickets.stream().map(Ticket::toString).collect(joining(","))});
            //Global
            SPreportDimension globalReport = new SPreportDimension(SPreportDimension.Dimension.GROUP);
//            globalReport.setAddedAst();
//            globalReport.setAllIncomplete();
//            globalReport.setCias();
            globalReport.setComplete(completedTickets.parallelStream().map(Ticket::getPoints).reduce(0d, Double::sum));
            globalReport.setAllComplete(globalReport.getComplete());
//            globalReport.setCompleteAst();
            globalReport.setEstimated(sprintTickets.parallelStream().map(Ticket::getPoints).reduce(0d,Double::sum));
            globalReport.setIdeal(pointsxSprint*developers.size());
            globalReport.setIncomplete(globalReport.getEstimated()-globalReport.getComplete());
//            globalReport.setRemoved();
            spreportDims.add(globalReport);

            info.setReports(spreportDims);
            resp.add(info);
        }
        return resp;
    }

    /**
     * Trae la lista de tickets en un tablero de JIRA. Hace uso de la
     * {@link #ticketCache cache}
     *
     * @param JIRAid
     * @return
     * @throws Exception
     */
    public static List<Ticket> getBoardIssues(String JIRAid) throws Exception {
        String url = AntSMUtilites.parse("[:JIRA_home]") + "/rest/agile/1.0/board/" + JIRAid + "/issue?fields=key";
        JSONObject jsonObj = Utils.JSONRequest(url);
        JSONArray issues = (JSONArray) jsonObj.get("issues");
//        log.info("collecting tickets");
        List<Ticket> tickets = (List<Ticket>) issues.stream()
                .map(e -> ((JSONObject) e).get("key"))
                .map(toTicket)
                .collect(toList());
        setTicketsSprints(tickets, jsonObj, JIRAid);
        return tickets;
    }

    /**
     * Attention: la consulta para obtener el JSON que llega acá debería
     * contener expand=changelog
     *
     * @param tickets
     * @param jsonObj
     * @param JIRAid
     */
    private static void setTicketsSprints(List<Ticket> tickets, JSONObject jsonObj, String JIRAid) {
        //Estados de entrada y salida al tablero
        List<String> entryStatuses = getEntryStatuses(JIRAid);
//        log.log(Level.INFO, "entry:{0}", entryStatuses.stream().collect(joining(",")));
        List<String> exitStatuses = getExitStatuses(JIRAid);
//        log.log(Level.INFO, "exit:{0}", exitStatuses.stream().collect(joining(",")));
//        log.info(""+tickets.size());
        tickets.parallelStream().forEach(ticket -> {
            if(!ticket.getSprints().isEmpty())
                return;
//            log.info(ticket.getKey());
            //Sprint(s)
            JSONObject changeLog;
            try {
                changeLog = getChangelog(ticket.getKey());
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
                return;
            }
            JSONArray histories = (JSONArray) changeLog.get("histories");
//            log.info("histories:" + histories.size());
            Optional<Date> entryStatusChange = filterHistoryStatuses(histories, entryStatuses);
            Optional<Date> exitStatusChange = filterHistoryStatuses(histories, exitStatuses);
            //Encontró la fecha en que entró al kanban?
            if (entryStatusChange.isPresent()) {
                final Date entryDate = entryStatusChange.get();
//                log.log(Level.INFO, "entry: {0}", entryDate);
                Optional<Integer> startSprintMatch = findMatchingSprint(entryDate);
                if (startSprintMatch.isPresent()) {
                    int startSprint, finalSprint = -1;
                    startSprint = startSprintMatch.get();
//                    log.info("start sprint:" + startSprint);
                    if (exitStatusChange.isPresent()) {
                        Optional<Integer> endSprintMatch = findMatchingSprint(exitStatusChange.get());
                        if (endSprintMatch.isPresent()) {
                            finalSprint = endSprintMatch.get();
                        }
                        if (finalSprint == -1) {
                            endSprintMatch = findMatchingSprint(new Date());
                            if (endSprintMatch.isPresent()) {
                                finalSprint = endSprintMatch.get();
                            }
                        }
                        List<Integer> sprints = new LinkedList<>();
                        sprints.add(startSprint);
//log.info(ticket.getKey()+"["+startSprint+" to "+finalSprint+"]");
                        if (finalSprint != -1) {
                            if (finalSprint >= startSprint) {
                                for (int sprint = startSprint + 1; sprint <= finalSprint; sprint++) {
                                    sprints.add(sprint);
                                }
                            } else {
                                for (int sprint = startSprint + 1; sprint <= 26; sprint++) {
                                    sprints.add(sprint);
                                }
                                for (int sprint = 1; sprint <= finalSprint; sprint++) {
                                    sprints.add(sprint);
                                }
                            }
                        }
                        ticket.setSprints(sprints);
//log.info(ticket.getKey()+" ["+sprints.stream().map(i->""+i).collect(joining(","))+"]");
                    } else {
                        log.fine("No aparece fin de estado para el ticket " + ticket.getKey() + " y los estados " + exitStatuses.stream().collect(joining(",")));
                    }
                }
            } else {
                log.log(Level.SEVERE, "Couldn''t get entry date of ticket {0} in states {1}", new Object[]{ticket.getKey(), entryStatuses.stream().collect(joining(","))});
            }
        });
    }

    public static List<String> getEntryStatuses(String JIRAid) {
        final List<String> defaultEntryStatuses = Arrays.asList(sysProps.getProperty("JIRA.kanban.startStatus").toUpperCase().split(","));
        String stKey = "JIRA.kanban." + JIRAid + ".startStatus";
        return sysProps.containsKey(stKey)
                ? Arrays.asList(sysProps.getProperty(stKey).toUpperCase().split(","))
                : defaultEntryStatuses;
    }

    private static JSONObject getChangelog(String key) throws InvalidParamException, Exception {
        final String url = AntSMUtilites.parse("[:JIRA_home]") + "/rest/api/2/issue/" + key + "?expand=changelog&fields=%22%22";
        return (JSONObject) Utils.JSONRequest(url).get("changelog");
    }

    /**
     * Attention: por facilidad, se manejan estados sólo en mayúscula
     * @param JIRAid
     * @return 
     */
    public static List<String> getExitStatuses(String JIRAid) {
        String stKey;
        List<String> statuses;
        //Salida del tablero
        final List<String> defaultExitStatuses = Arrays.asList(sysProps.getProperty("JIRA.kanban.endStatus").toUpperCase().split(","));
        stKey = "JIRA.kanban." + JIRAid + ".endStatus";
        statuses = sysProps.containsKey(stKey)
                ? Arrays.asList(sysProps.getProperty(stKey).toUpperCase().split(","))
                : defaultExitStatuses;
        return statuses;
    }

    private static Optional<Integer> findMatchingSprint(Date sampleDate) {
        final Optional<Integer> resp = sprintRanges.entrySet().parallelStream()
                .filter(r -> {
                    final Range<Date> range = r.getKey();
                    final Date start = range.getStart();
                    return (start.compareTo(sampleDate) <= 0
                            && range.getEnd().compareTo(sampleDate) >= 0);
                })
                .map(Map.Entry::getValue)
                .findFirst();
        return resp;
    }

    private static Optional<Date> filterHistoryStatuses(JSONArray histories, List<String> statuses) {
        Optional<Date> entryStatusChange = histories.parallelStream()
                .map(h -> {
                    //La inicialización de esta variable (df), falla por fuera del bucle
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    JSONObject history = (JSONObject) h;
                    JSONArray items = (JSONArray) history.get("items");
                    List<JSONObject> changes = (List<JSONObject>) items.parallelStream().filter(i -> {
                        JSONObject item = (JSONObject) i;
                        final String field = (String) item.get("field");
                        final String fieldValue = (String) item.get("toString");
                        final boolean matches = field.equals("status")
                                && statuses.contains(fieldValue.toUpperCase());
//if (DEBUG) {
//    log.log(Level.INFO, "{0}:{1}={2}", new Object[]{field, fieldValue, matches});
//}
                        return matches;
                    }).collect(toList());
//if (DEBUG) {
//    log.info(changes.stream().map(JSONObject::toString).collect(joining(",")));
//}
                    Date created = null;
                    if (!changes.isEmpty()) {
                        try {
                            created = df.parse((String) history.get("created"));
                        } catch (ParseException | NumberFormatException ex) {
                            log.log(Level.SEVERE, (String) history.get("created"), ex);
                            return null;
                        }
                    }
//if (DEBUG) {
//    log.info("returning " + created);
//}
                    return created;
                }).filter(d -> d != null)
                .sorted()
                .findFirst();
        return entryStatusChange;
    }

    private static List<Ticket> collectEpics(String JIRAid) throws InvalidVarNameException, InvalidParamException, Exception {
        List<Ticket> resp = new LinkedList<>();
        String url = AntSMUtilites.parse("[:JIRA_home]") + "/rest/agile/1.0/board/" + JIRAid + "/epic";
        JSONObject jsonObj = JSONRequest(url);
        JSONArray epics = (JSONArray) jsonObj.get("values");
        if (epics != null) {
            resp = (List<Ticket>) epics.stream()
                    .map(v -> {
                        JSONObject jsonEpic = (JSONObject) v;
                        Ticket ticket = new Ticket();
                        ticket.setKey((String) jsonEpic.get("key"));
                        try {
                            ticket.setURL(AntSMUtilites.parse("[:JIRA_home]") + "/browse/" + ticket.getKey());
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                        ticket.setSummary((String) jsonEpic.get(summaryField));
                        return ticket;
                    }).collect(toList());
        }
        return resp;
    }
}
