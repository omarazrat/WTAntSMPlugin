package antsm.com.tests.tests;

import java.io.File;
import org.junit.Test;

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
/**
 *
 * @author nesto
 */
public class DummyTest {

    public DummyTest() {
    }

    @Test
    public void hello() {
        String offset="     51.0          34.0        19.0        0      2.0        2.0            0      32.0            19.0     0";
        final String[] tokens = offset.split("\\w");
        final String pythonPath = "pythonPath";
        final String SLASH = System.getProperty("file.separator");
        String genCommand = pythonPath + "{slash}python {base}{slash}sprint-report{slash}sprint-report.py  --output CONSOLE --team {team} "
                + "--user {JIRA_USER} --password {JIRA_PWD} --sprint {sprint}"
                        .replace("{JIRA_USER}", "JIRA_USER")
                        .replace("{JIRA_PWD}", "JIRA_PWD");
        genCommand = genCommand.replace("{base}", "spreportPath")
                .replace("{team}", "JIRAid");

        while (genCommand.contains("{slash}")) {
            genCommand = genCommand.replace("{slash}", SLASH);
        }
        System.out.println(genCommand);
    }
}
