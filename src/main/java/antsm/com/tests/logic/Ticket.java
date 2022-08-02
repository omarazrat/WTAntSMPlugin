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
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author nesto
 */
@Data
@EqualsAndHashCode(of = "key")
public final class Ticket {
    public enum TYPE{
        TASK,
        SUBTASK,
        STORY,
        BUG
    };
    private String key;
    private String URL;
    private TYPE type;
    private String assignee;
    private String status;
    private String epic;
    private List<String> fixEngineers = new LinkedList<>();
    private List<String> teams = new LinkedList<>();
    
    public boolean matchesTeam(String teamName){
        return teams.contains(teamName);
    }
    public boolean matchesFE(String fixEngineer){
        return fixEngineers.contains(fixEngineer);
    }
}
