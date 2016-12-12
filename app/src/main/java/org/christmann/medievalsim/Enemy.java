package org.christmann.medievalsim;


import java.io.Serializable;

/**
 * Created by Guilherme on 28/11/2016.
 * This is an enemy. He will bite your ass.
 */

@SuppressWarnings("ALL")
class Enemy implements Serializable{
    private String name;        // Identifies "species"
    private int maxhp;          // max hp of the enemy
    private int currentHP;      // current hp of the enemy
    private int level;          // level of the enemy
    private int atk;            // modifier when dealing damage
    private int def;            // modifier when receiving damage
    private int spd;            // modifier to determine who plays first

    private boolean isAlive;    // determines if a monster is alive (derp)

    private double lat;          // latitude
    private double lng;          // longitude

    public Enemy(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }
}
