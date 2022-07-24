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

import java.util.Objects;
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

    public CapacityInfo(String teamName, int sprint) {
        super(teamName, sprint);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
