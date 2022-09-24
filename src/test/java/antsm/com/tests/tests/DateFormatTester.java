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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author nesto
 */
public class DateFormatTester {
    
    public DateFormatTester() {
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
     @Test
     public void hello() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

         for(String entry : new String[]{"2022-07-22T14:33:32.468+0000","2022-07-26T16:38:01.318+0000","2022-07-27T18:15:23.820+0000"
                 ,"2022-07-27T16:59:54.794+0000","2022-07-14T15:44:34.631+0000","2022-07-27T17:00:14.706+0000","2022-08-09T14:23:09.280+0000"
         ,"2022-08-15T14:06:24.130+0000","2022-08-09T14:56:33.753+0000","2021-06-17T18:31:42.192+0000","2021-06-29T15:50:12.409+0000"
         ,"2021-05-28T14:33:04.525+0000","2021-06-21T15:49:40.470+0000","2019-12-10T16:23:14.298+0000","2020-11-30T12:53:12.963+0000"
         ,"2020-12-21T14:04:07.575+0000","2021-05-12T13:55:00.777+0000"}){
            try {
                System.out.println(df.parse(entry));
            } catch (ParseException ex) {
                Logger.getLogger(DateFormatTester.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
     }
}
