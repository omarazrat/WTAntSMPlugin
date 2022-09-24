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

import antsm.com.tests.logic.Ticket;
import antsm.com.tests.plugins.AntSMUtilites;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author nesto
 */
public abstract class Utils {

    private static Logger log = Logger.getLogger("WebAppTester");

    public static JSONObject JSONRequest(final String url) throws ParseException, Exception, IOException, URISyntaxException, InterruptedException {
//        log.info("loading "+url);
        String valueToEncode = JIRAReportHelper.getJiraUser() + ":" + AntSMUtilites.parse(JIRAReportHelper.getJiraPassword());
//        log.info(valueToEncode);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(new URI(url))//)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes()))
                .GET().build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();
//                log.info(response.body());
        JSONObject jsonObj = (JSONObject) parser.parse(response.body());
        return jsonObj;
    }

    public static Double sumCompletedPointsByDeveloper(final List<Ticket> sprintTickets, final List<String> exitStatuses,
             String developer, int sprint) {
        return sprintTickets
                .parallelStream()
                .map(ticket -> {
                    if (!closedInSprint(ticket, exitStatuses, sprint)) {
                        return 0d;
                    }
                    final List<String> fixEngineers = ticket.getFixEngineers();
                    if (fixEngineers.contains(developer)) {
                        return ticket.getPoints() / fixEngineers.size();
                    }
                    if (ticket.getAssignee().equals(developer)) {
                        return ticket.getPoints();
                    }
                    return 0d;
                }).reduce(0d, Double::sum);
    }

    public static Double sumPointsByDeveloper(final List<Ticket> sprintTickets, String developer) {
        return sprintTickets
                .parallelStream()
                .map(ticket -> {
                    final List<String> fixEngineers = ticket.getFixEngineers();
                    if (fixEngineers.contains(developer)) {
                        return ticket.getPoints() / fixEngineers.size();
                    }
                    if (ticket.getAssignee().equals(developer)) {
                        return ticket.getPoints();
                    }
                    return 0d;
                }).reduce(0d, Double::sum);
    }

    public static boolean closedInSprint(Ticket ticket, List<String> exitStatuses, int sprint) {
        final int nextSprint = sprint == 26 ? 1:sprint + 1;
        //Si se cerró en este sprint
        return ticket.matchesSprint(sprint) && !ticket.matchesSprint(nextSprint)
                && //Si el estadu cuadra
                exitStatuses.contains(ticket.getStatus());
    }

}
