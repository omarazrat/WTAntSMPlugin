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
package antsm.com.tests.tests;

import antsm.com.tests.plugins.AntSMUtilites;
import antsm.com.tests.utils.JIRAReportHelper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import static java.util.jar.Attributes.Name.CONTENT_TYPE;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.remote.http.HttpResponse;

/**
 *
 * @author nesto
 */
public class ElDoradoDataCollector {

    public ElDoradoDataCollector() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
//     @Test
    public void hello() throws IOException {
        String host = "https://jira.bbpd.io";
        String username = "narias";
        String pwd = "t34A18fxz,6.M,IO";
    }
}
