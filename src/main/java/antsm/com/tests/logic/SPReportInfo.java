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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @author nesto
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class SPReportInfo  extends AbstractTeamSprintInfo{
    private double estimated,allComplete ,incomplete,removed,addedAst,completeAst,incompleteAst,complete,allIncomplete,cias,ideal;

    public SPReportInfo(String teamName, int sprint) {
        super(teamName, sprint);
    }
    
    public double getSpVar(){
        if(estimated==0)
            return 0;
        return (removed+addedAst)/estimated;
    }	
    public double getUtRatio(){
        if(ideal==0)
            return 0;
        return estimated/ideal;
    }	
    public double getCompRatio(){
        if(estimated==0)
            return 0;
        return allComplete/estimated;
    }
     public double getRealValue(){
        double denominador = allComplete+removed+cias+allIncomplete;
        if(denominador==0)
                return 0;
        return (allComplete+cias)/denominador;
     }
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
