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

import antsm.com.tests.logic.SPReportInfo;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author nesto
 */
public class SPReportInfoTest {
    
    public SPReportInfoTest() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
     @Test
     public void testEquals() {
         SPReportInfo info1 = new SPReportInfo("Beta", 12),
                 info2 = new SPReportInfo("Beta", 12),
                 info3 = new SPReportInfo("Beta",13),
                 info4 = new SPReportInfo("Gamma", 12);
         assertEquals(info1, info2);
         assertNotEquals(info1, info3);
         assertNotEquals(info1, info4);
     }
}
