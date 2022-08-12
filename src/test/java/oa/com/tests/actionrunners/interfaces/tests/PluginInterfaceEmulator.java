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
package oa.com.tests.actionrunners.interfaces.tests;

import antsm.com.tests.plugins.AntSMUtilites;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import oa.com.tests.actionrunners.exceptions.InvalidParamException;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import oa.com.tests.actionrunners.interfaces.PluginInterface;
import oa.com.tests.actionrunners.interfaces.listeners.PluginStoppedListener;
import oa.com.tests.plugins.AbstractDefaultPluginRunner;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author nesto
 */
public class PluginInterfaceEmulator implements PluginInterface{

    private WebDriver driver ;

    public PluginInterfaceEmulator() {
        driver = new FirefoxDriver();
        AntSMUtilites plugin = new AntSMUtilites();
        plugin.setActionManager(this);
    }
    
    @org.junit.AfterClass
    public void destroy(){
        driver.close();
    }
    
    @Override
    public List<Exception> exec(File file, Logger log) throws InvalidVarNameException, FileNotFoundException, IOException, InvalidParamException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public void go(String url){
        driver.get(url);
    }
    
    @Override
    public String parse(String pwdString) throws InvalidVarNameException, InvalidParamException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void run(String command) throws IOException, InvalidVarNameException, InvalidParamException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public void registerPluginListener(AbstractDefaultPluginRunner plugin, PluginStoppedListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void registerStart(AbstractDefaultPluginRunner plugin) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void registerStop(AbstractDefaultPluginRunner plugin) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isRunning(AbstractDefaultPluginRunner plugin) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
