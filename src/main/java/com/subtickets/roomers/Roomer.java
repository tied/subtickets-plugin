package com.subtickets.roomers;

public class Roomer {
    private Roomer() {
    }

    public String fio;
    public String holderInfo;
    public Float participantIndex;
    public String[] owned_Autos;
    public String[] owned_Doors;
    public Double amount;
    public Float totalSquire;

    public String[] getAutos() {
        return owned_Autos;
    }

    public String[] getDoors() {
        return owned_Doors;
    }
}
