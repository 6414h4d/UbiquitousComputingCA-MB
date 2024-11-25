package com.example.microbitca;

/*
Add Firebase connectivity
When the app is started, attempt to connect to the database.
If the connection fails, create a toast to alert the user

 */
public class PunchPowerModel {
    public String punchPower;
    public String userId;
    
    public PunchPowerModel(){}
    
    public PunchPowerModel(String punchPower,String userId){
        this.punchPower = punchPower;
        this.userId = userId;
    }
    
}
