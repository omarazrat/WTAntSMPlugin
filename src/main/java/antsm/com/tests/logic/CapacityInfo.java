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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
@EqualsAndHashCode(callSuper = false)
public class CapacityInfo extends AbstractTeamSprintInfo{
    private double effectiveHours;
    private int developers;
    private double coefficient;
    private double ideal;
    private double commited;

    private Map<String,Double> personalCoefficients = new HashMap<>();
    private static final Logger log = Logger.getLogger("WebAppTester");
    
    public CapacityInfo(String teamName, int sprint) {
        super(teamName, sprint);
    }

    public Double getCoefficient(String userName) {
//        for (String key : personalCoefficients.keySet()) {
//            log.log(Level.INFO, "{0}={1}", new Object[]{key, personalCoefficients.get(key)});
//        }
        return personalCoefficients.get(userName);
    }

    public Double put(String userName, Double coefficient) {
        return personalCoefficients.put(userName, coefficient);
    }

    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
