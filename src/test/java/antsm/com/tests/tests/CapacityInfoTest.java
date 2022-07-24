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

import antsm.com.tests.logic.CapacityInfo;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author nesto
 */
public class CapacityInfoTest {
    private Logger log = Logger.getLogger("WebAppTester");
    
    public CapacityInfoTest() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
     @Test
     public void doIt() {
         CapacityInfo info1 = new CapacityInfo("Alpha", 1),
                 info2 = new CapacityInfo("Alpha", 1),
                 info3 = new CapacityInfo("Alpha", 2),
                 info4 = new CapacityInfo("Chicha", 2);
         info1.setIdeal(12);
         info3.setIdeal(112);
         log.info(info1.toString());
         log.info(info2.toString());
         log.info(info3.toString());

         assert (info1.equals(info2));
         assert (info2.equals(info2));
         assert (!info2.equals(info3));
         assert (!info2.equals(info4));
     }
}
