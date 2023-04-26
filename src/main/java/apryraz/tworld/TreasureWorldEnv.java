

package apryraz.tworld;

import java.util.ArrayList;





public class TreasureWorldEnv {
    /**
     * X,Y position of Treasure and world dimension
     **/
    int TreasureX, TreasureY, WorldDim;


    /**
     * Class constructor
     *
     * @param dim dimension of the world
     * @param tx  X position of Treasure
     * @param ty  Y position of Treasure
     **/
    public TreasureWorldEnv(int dim, int tx, int ty) {
        TreasureX = tx;
        TreasureY = ty;
        WorldDim = dim;
    }


    /**
     * Process a message received by the TFinder agent,
     * by returning an appropriate answer
     * This version only process answers to moveto and detectsat messages
     *
     * @param msg message sent by the Agent
     * @return a msg with the answer to return to the agent
     **/
    public AMessage acceptMessage(AMessage msg) {
        AMessage ans = new AMessage("voidmsg", "", "", "");

        msg.showMessage();
        if (msg.getComp(0).equals("moveto")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));

            if(withinLimits(nx, ny)) {
                ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2), "");
            } else {
                ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2), "");
            }

        } else if (msg.getComp(0).equals("detected")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));

            if (withinLimits(nx, ny)) {
                ans = new AMessage("detected", msg.getComp(1), msg.getComp(2), sensorValueAssignment(Integer.parseInt(msg.getComp(1)), Integer.parseInt(msg.getComp(2))));
            } else {
                ans = new AMessage("notdetectsat", msg.getComp(1), msg.getComp(2), "");
            }
        }

        return ans;
    }

    String sensorValueAssignment(int x, int y) {
        if(sensorValue1(x, y)) {
            return "1";
        } else if (sensorValue2(x, y)) {
            return "2";
        }
        return "3";
    }

    boolean sensorValue1(int x, int y) {
        int[][] sensor = {{x, y - 1}, {x, y}, {x, y + 1}, {x - 1, y}, {x + 1, y}};
        for(int i = 0; i < sensor.length; i++) {
            if(TreasureX == sensor[i][0] && TreasureY == sensor[i][1]) {
                return true;
            }
        }
        return false;
    }

    boolean sensorValue2(int x, int y) {
        int[][] sensor = {{x + 1, y + 1}, {x + 1, y - 1}, {x - 1, y - 1}, {x - 1, y + 1}};
        for(int i = 0; i < sensor.length; i++) {
            if(TreasureX == sensor[i][0] && TreasureY == sensor[i][1]) {
                return true;
            }
        }
        return false;
    }

        /**
         * Check if position x,y is within the limits of the
         * WorldDim x WorldDim   world
         *
         * @param x  x coordinate of agent position
         * @param y  y coordinate of agent position
         *
         * @return true if (x,y) is within the limits of the world
         **/
        public boolean withinLimits(int x, int y){
            return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
        }

    }