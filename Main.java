//V4 5/9/2023

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        int MAX_SIM_TIME = 200;
        double START_POS = 50; // 65km approx Radar Target Visibility between 150m and 15 m

        // Initialize the SAM site and an enemy target
        SamSite site1 = new SamSite();
        Enemy enemy1 = new Enemy(START_POS);
        List<Missile> missileList = new ArrayList<>();

        // Use StringBuilder to accumulate data
        StringBuilder outputBuilder = new StringBuilder();

        // Simulation loop
        for (int time = 0; time < MAX_SIM_TIME; time++) {
            site1.samLoop(enemy1, time); // Run SAM site's logic loop

            // Fire a missile if SAM site decides to
            if (site1.isFireDecision()) {
                missileList.add(site1.fireMissile(enemy1, time));
            }

            // Iterate through active missiles
            Iterator<Missile> missileIterator = missileList.iterator();
            int activeMissiles = 0;
            while (missileIterator.hasNext()) {
                Missile missile = missileIterator.next();
                missile.missileLoop(time); // Run missile's logic loop

                if ("Active".equals(missile.getStatus())) {
                    activeMissiles++;
                }
            }

            // Update the SAM site with the number of missiles currently in flight
            site1.setMissilesInFlight(activeMissiles);

            enemy1.enemyLoop(); // Move the enemy target


            // Append the status and position of the enemy and each missile to the StringBuilder
            outputBuilder.append("Time: ").append(time).append("s\n");
            outputBuilder.append("Enemy Position: ").append(enemy1.getPosition()).append("km. Status: ").append(enemy1.isAlive() ? "Alive" : "Destroyed").append("\n");
            for (Missile missile : missileList) {
                outputBuilder.append("Missile ").append(missile.getName()).append(" Position: ").append(missile.getPosition()).append("km. Status: ").append(missile.getStatus()).append("\n");
            }

            // End condition
            if (enemy1.isAlive() && enemy1.getPosition()<=0) {
                System.out.println("<<< Enemy destroyed SAM site at time: " + time + "s! >>>");
                break;
            }
                else if(!enemy1.isAlive() && activeMissiles==0){
                break;}
        }

        // Write the content of outputBuilder to a file
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("simulation_output.txt"))) {
            bufferedWriter.write(outputBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void exportDataToTxt(String data) {
        try {
            FileWriter writer = new FileWriter("simulation_output.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);

            bufferedWriter.write(data);
            bufferedWriter.newLine();

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


class SamSite {
    private int ammo = 10;
    private int missilesInFlight = 0;
    private double acquisitionLevel = 0;
    private boolean fireDecision = false;
    int missileName = 0;
    private final double MAX_RANGE = 45;
    private final double DETECTION_CHANCE = 0.1;
    private final int MAX_MISSILESINFLIGHT = 2;

    // SAM site's main loop
    public void samLoop(Enemy enemy, int time) {
        // Only run if the enemy is alive
        if (enemy.isAlive()) {
            // Detect, acquire, and decide to fire
            if (detectTarget() && !enemy.isDetected()) {
                enemy.setDetected(true, time);
            }

            if (enemy.isDetected() && !enemy.isAcquired()) {
                if (acquireTarget(time)) {
                    enemy.setAcquired(true, time);
                }
            }

            // Decide to fire based on target acquisition, missiles in flight and range
            fireDecision = enemy.isAcquired() && missilesInFlight < MAX_MISSILESINFLIGHT && enemy.getPosition()<=MAX_RANGE && ammo>0;
        } else {
            // Reset the acquisition if the target is not alive
            acquisitionLevel = 0;
            fireDecision = false;
        }
    }

    // Try to detect the target (random chance)
    private boolean detectTarget() {
        return Math.random() <= DETECTION_CHANCE;
    }

    // Try to acquire the target (cumulative over time)
    private boolean acquireTarget(int time) {
        if (acquisitionLevel < 100) {
            acquisitionLevel += 20;
            System.out.println("Acquiring Target: " + acquisitionLevel + "%");
            return false;
        } else {
            return true;
        }
    }

    // Fire a missile at the target
    public Missile fireMissile(Enemy enemy, int time) {
        ammo--;
        missileName++;
        System.out.println("Missile " + missileName + " Fired at time: " + time + "s, Target distance: " + enemy.getPosition() + "km");
        System.out.println("Remaining ammo: " + ammo);
        return new Missile(enemy, missileName);
    }

    // Check if SAM site has decided to fire
    public boolean isFireDecision() {
        return fireDecision;
    }

    public void setMissilesInFlight(int missilesInFlight) {
        this.missilesInFlight = missilesInFlight;
    }
}

class Missile {
    private double position = 0;
    private String status = "Active";
    private final double SPEED = 0.8; // ~mach 2.5
    private final double HIT_CHANCE = 0.5;
    private final double MAX_RANGE = 46;
    private Enemy enemy = null;
    private int name = 0;

    public Missile(Enemy enemy, int name) {
        this.enemy = enemy;
        this.name = name;
    }

    public void missileLoop(int time) {
        if ("Active".equals(status)) {
            if (checkDistance() ) {
                if (checkHit(time)) {
                    setStatus("Hit", time);
                    if (enemy.isAlive()){
                    enemy.setAlive(false, time);
                    }
                } else {
                    setStatus("Missed", time);
                }
            }
        }
        // move if not hit until max range
        if (!"Hit".equals(status)){
            if (position <MAX_RANGE){
            move();}
        }
    }

    private void move() {
        position += SPEED;
    }

    private boolean checkDistance() {
        return enemy.getPosition() - position <= 0;
    }

    private boolean checkHit(int time) {
        return Math.random() < HIT_CHANCE;
    }

    public String getStatus() {
        return status;
    }

    public double getPosition() {
        return position;
    }

    public int getName() {
        return name;
    }

    private void setStatus(String status, int time) {
        this.status = status;
        System.out.println("Missile " + name + " "+ status + " at time: " + time + "s");
    }
}

class Enemy {
    private double position;
    private boolean alive = true;
    private boolean detected = false;
    private boolean acquired = false;
    private final double SPEED = 0.5; // ~mach 1.5

    public Enemy(double position) {
        this.position = position;
    }

    public void enemyLoop() {
        if (alive) {
            this.move();
        }
        else{
            detected = false;
            acquired = false;
        }

    }

    private void move() {
        position -= SPEED;
    }

    public boolean isAlive() {
        return alive;
    }

    public double getPosition() {
        return position;
    }

    public boolean isDetected() {
        return detected;
    }

    public boolean isAcquired() {
        return acquired;
    }

    public void setAlive(boolean alive, int time) {
        this.alive = alive;
        if (!alive) {
            System.out.println("Target destroyed at time: " + time + "s, Target position: " + position + "km");
        }
    }

    public void setDetected(boolean detected, int time) {
        this.detected = detected;
        if (detected) {
            System.out.println("Target is Detected at time: " + time + "s");
        }
    }

    public void setAcquired(boolean acquired, int time) {
        this.acquired = acquired;
        if (acquired) {
            System.out.println("Target is Acquired at time: " + time+ "s");
        }
    }
}
