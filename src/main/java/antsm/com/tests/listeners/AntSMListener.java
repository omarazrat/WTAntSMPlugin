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
package antsm.com.tests.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import antsm.com.tests.swing.ControlPanel;

/**
 *
 * @author nesto
 */
public class AntSMListener implements ActionListener{

    private Logger log = Logger.getLogger("WebAppTester");
    @Override
    public void actionPerformed(ActionEvent e) {
        log.info("SM plugin up and running");
        ControlPanel cp = new ControlPanel();
        cp.setVisible(true);
    }
    
}
