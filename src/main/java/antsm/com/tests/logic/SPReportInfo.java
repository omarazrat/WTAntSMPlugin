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
package antsm.com.tests.logic;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @author nesto
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SPReportInfo extends AbstractTeamSprintInfo {

    private static Logger log = Logger.getLogger("WebAppTester");
    private List<SPreportDimension> reports = new LinkedList<>();

    ;
    public SPReportInfo(String teamName, int sprint) {
        super(teamName, sprint);
    }

    public SPreportDimension get(SPreportDimension.Dimension dimension) {
        Optional<SPreportDimension> match = reports.stream().filter(r -> r.getDimension().equals(dimension))
                .findAny();
        if (match.isEmpty()) {
            return null;
        }
        return match.get();
    }

    public List<SPreportDimension> getMany(SPreportDimension.Dimension dimension) {
        return reports.stream().filter(r -> r.getDimension().equals(dimension))
                .collect(toList());
    }

    public void add(SPreportDimension report) {
//        reports.remove(report);
        reports.add(report);
    }
}
