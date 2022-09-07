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

import antsm.com.tests.logic.SPReportInfo;
import antsm.com.tests.logic.SPreportDimension;
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
    public void hello() {
        SPReportInfo report1 = new SPReportInfo("Tequendama", 17);
        for (int id = 1; id < 10; id++) {
            SPreportDimension dimReport = new SPreportDimension(SPreportDimension.Dimension.DP);
            dimReport.setId("" + id);
            dimReport.setComplete(Double.valueOf(Math.random() * 20).intValue());
            dimReport.setAddedAst(Double.valueOf(Math.random() * 5).intValue());
            dimReport.setAllComplete(dimReport.getComplete() + Double.valueOf(Math.random() * 3).intValue());
            dimReport.setEstimated(Double.valueOf(Math.random() * 10).intValue() + 20);
            dimReport.setAllIncomplete(dimReport.getEstimated() - dimReport.getAllComplete());
            dimReport.setIdeal(dimReport.getEstimated() +Math.random() * 5);
            dimReport.setRemoved(Double.valueOf(Math.random() * 5).intValue());
            report1.add(dimReport);
        }
    }
}
