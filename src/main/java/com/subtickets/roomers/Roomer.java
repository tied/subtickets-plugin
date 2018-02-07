package com.subtickets.roomers;

public class Roomer {
    private Roomer() {
    }

    public String fio;
    public String holderInfo;
    public Double participantIndex;
    public String[] owned_Autos;
    public String[] owned_Doors;
    public Double amount;
    public Double totalSquire;

    public String[] getAutos() {
        return owned_Autos;
    }

    public String[] getDoors() {
        return owned_Doors;
    }

    public Integer getAutosToCount() {
        return getAutos().length;
    }

    public Double getDoorsToCount() {
        return getDoors().length * participantIndex;
    }

    public Double getSquareToCount() {
        return totalSquire;
    }
}
