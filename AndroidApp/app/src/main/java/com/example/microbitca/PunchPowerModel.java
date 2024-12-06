package com.example.microbitca;

/*
Add Firebase connectivity
When the app is started, attempt to connect to the database.
If the connection fails, create a toast to alert the user

 */
public class PunchPowerModel {
    public String userId;
    public String punchPower;

    public PunchPowerModel(String userId,String punchPower){
        this.userId = userId;
        this.punchPower = punchPower;
    }
    
}
