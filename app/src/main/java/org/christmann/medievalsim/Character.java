package org.christmann.medievalsim;

/**
 * Created by Guilherme on 28/11/2016.
 * Class that will stores character information and stats
 */

class Character {
    boolean online;
    String displayName;

    private int maxhp;          // max hp of the enemy
    private int currentHP;
    private int level;          // level of the enemy
    private int atk;            // modifier when dealing damage
    private int def;            // modifier when receiving damage
    private int spd;            // modifier to determine who plays first

    private double lat;          // latitude
    private double lng;          // longitude

    public Character(){
        // Empty constructor so Firebase can do its thing
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getMaxhp() {
        return maxhp;
    }

    public void setMaxhp(int maxhp) {
        this.maxhp = maxhp;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public int getDef() {
        return def;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public int getSpd() {
        return spd;
    }

    public void setSpd(int spd) {
        this.spd = spd;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
