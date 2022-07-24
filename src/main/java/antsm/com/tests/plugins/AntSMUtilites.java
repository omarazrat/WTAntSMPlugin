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
package antsm.com.tests.plugins;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import antsm.com.tests.listeners.AntSMListener;
import antsm.com.tests.swing.ControlPanel;
import static antsm.com.tests.utils.ConfluenceHelper.getCapacityURL;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraPassword;
import static antsm.com.tests.utils.JIRAReportHelper.getJiraUser;
import antsm.com.tests.utils.PythonReportHelper;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import oa.com.tests.plugins.AbstractDefaultPluginRunner;
import org.openqa.selenium.WebDriver;
import oa.com.tests.actionrunners.interfaces.PluginInterface;

/**
 *
 * @author nesto
 */
public class AntSMUtilites extends AbstractDefaultPluginRunner {

    private static AntSMUtilites instance;
    private static final Logger log = Logger.getLogger("WebAppTester");

    public AntSMUtilites() {
        instance = this;
//        final File pluginFolder = new File("lib/antsm/output");
//        if (!pluginFolder.exists()) {
//            pluginFolder.mkdir();
//        }
    }

    @Override
    public String getButtonActionCommand() {
        return "Utilitarios SM";
    }

    @Override
    public ActionListener getActionListener() {
        return new AntSMListener();
    }

    @Override
    public void setActionManager(PluginInterface actionManager) {
        super.setActionManager(actionManager);
        instance = this;
    }

    @Override
    public Icon getIcon() throws IOException {
        final BufferedImage img = ImageIO.read(getClass().getResource("/antsm/imgs/sm.png"));
        return new ImageIcon(img);
    }

    public static void runTemplate(String fileName) throws InvalidVarNameException, IOException, InvalidParamException {
        runTemplate(fileName, null);
    }

    public static void runTemplate(String fileName, String teamName) throws InvalidVarNameException, IOException, InvalidParamException {
        final InputStream streamReader = ControlPanel.class.getResourceAsStream("/antsm/templates/" + fileName);
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        streamReader.transferTo(ostream);
        streamReader.close();
        ostream.close();
        String instructions = new String(ostream.toByteArray())
                .replace("{TEAM_URL}", teamName == null ? "" : getCapacityURL(teamName))
                .replace("{JIRA_USER}",getJiraUser())
                .replace("{JIRA_PWD}", getJiraPassword());
//        log.info("instructions:" + instructions);
        run(instructions);
    }

    public static Properties getConfigFile() {
        Properties sysProps = new Properties();
        try {
            sysProps.load(new FileReader("lib/antsm/antsmConfig.properties"));
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return sysProps;
    }
}
