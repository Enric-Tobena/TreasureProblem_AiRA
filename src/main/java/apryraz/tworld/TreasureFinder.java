

package apryraz.tworld;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;


/**
*  This agent performs a sequence of movements, and after each
*  movement it "senses" from the evironment the resulting position
*  and then the outcome from the smell sensor, to try to locate
*  the position of Treasure
*
**/
public class TreasureFinder {


    /**
     * The list of steps to perform
     **/
    ArrayList<Position> listOfSteps;
    /**
     * index to the next movement to perform, and total number of movements
     **/
    int idNextStep, numMovements;
    /**
     * Array of clauses that represent conclusiones obtained in the last
     * call to the inference function, but rewritten using the "past" variables
     **/
    ArrayList<VecInt> futureToPast = new ArrayList<VecInt>();
    /**
     * the current state of knowledge of the agent (what he knows about
     * every position of the world)
     **/
    TFState tfstate;
    /**
     * The object that represents the interface to the Treasure World
     **/
    TreasureWorldEnv EnvAgent;
    /**
     * SAT solver object that stores the logical boolean formula with the rules
     * and current knowledge about not possible locations for Treasure
     **/
    ISolver solver;
    /**
     * Agent position in the world
     **/
    int agentX, agentY;
    /**
     * Dimension of the world and total size of the world (Dim^2)
     **/
    int WorldDim, WorldLinealDim;

    /**
     * This set of variables CAN be use to mark the beginning of different subsets
     * of variables in your propositional formula (but you may have more sets of
     * variables in your solution or use totally different variables to identify
     * your different subsets of variables).
     **/


    /**
     * Offset of past possible treasure positions
     */

    int TreasurePastOffset;
    /**
     * Offset of future possible treasure positions
     */
    int TreasureFutureOffset;
    /**
     * Offset of sensor 1 positions
     */
    int DetectorOffset1;
    /**
     * Offset of sensor 2 positions
     */
    int DetectorOffset2;
    /**
     * Offset of sensor 3 positions
     */
    int DetectorOffset3;
    /**
     * Counter of literals
     */
    int actualLiteral;


    /**
     * The class constructor must create the initial Boolean formula with the
     * rules of the Treasure World, initialize the variables for indicating
     * that we do not have yet any movements to perform, make the initial state.
     *
     * @param WDim the dimension of the Treasure World
     **/
    public TreasureFinder(int WDim) {

        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        try {
            solver = buildGamma();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TreasureFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ContradictionException ex) {
            Logger.getLogger(TreasureFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        numMovements = 0;
        idNextStep = 0;
        System.out.println("STARTING TREASURE FINDER AGENT...");

        tfstate = new TFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        tfstate.printState();
    }

    /**
     * Store a reference to the Environment Object that will be used by the
     * agent to interact with the Treasure World, by sending messages and getting
     * answers to them. This function must be called before trying to perform any
     * steps with the agent.
     *
     * @param environment the Environment object
     **/
    public void setEnvironment(TreasureWorldEnv environment) {
        EnvAgent = environment;
    }


    /**
     * Load a sequence of steps to be performed by the agent. This sequence will
     * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
     * as objects of the class Position.
     *
     * @param numSteps  number of steps to read from the file
     * @param stepsFile the name of the text file with the line that contains
     *                  the sequence of steps: x1,y1 x2,y2 ...  xn,yn
     **/
    public void loadListOfSteps(int numSteps, String stepsFile) {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG. => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(TreasureFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(numSteps);
        for (int i = 0; i < numSteps; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     * Returns the current state of the agent.
     *
     * @return the current state of the agent, as an object of class TFState
     **/
    public TFState getState() {
        return tfstate;
    }

    /**
     * Execute the next step in the sequence of steps of the agent, and then
     * use the agent sensor to get information from the environment. In the
     * original Treasure World, this would be to use the Smelll Sensor to get
     * a binary answer, and then to update the current state according to the
     * result of the logical inferences performed by the agent with its formula.
     **/
    public void runNextStep() throws IOException, ContradictionException, TimeoutException {

        // Add the conclusions obtained in the previous step
        // but as clauses that use the "past" variables
        addLastFutureClausesToPastClauses();

        // Ask to move, and check whether it was successful
        processMoveAnswer(moveToNext());

        // Next, use Detector sensor to discover new information
        processDetectorSensorAnswer(DetectsAt());

        // Perform logical consequence questions for all the positions
        // of the Treasure World
        performInferenceQuestions();
        tfstate.printState();      // Print the resulting knowledge matrix
    }


    /**
     * Ask the agent to move to the next position, by sending an appropriate
     * message to the environment object. The answer returned by the environment
     * will be returned to the caller of the function.
     *
     * @return the answer message from the environment, that will tell whether the
     * movement was successful or not.
     **/
    public AMessage moveToNext() {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return new AMessage("NOMESSAGE", "", "", "");
        }
    }

    /**
     * Use agent "actuators" to move to (x,y)
     * We simulate this why telling to the World Agent (environment)
     * that we want to move, but we need the answer from it
     * to be sure that the movement was made with success
     *
     * @param x horizontal coordinate of the movement to perform
     * @param y vertical coordinate of the movement to perform
     * @return returns the answer obtained from the environment object to the
     * moveto message sent
     **/

    public AMessage moveTo(int x, int y) {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", Integer.toString(x), Integer.toString(y), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

    /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
     **/
    public void processMoveAnswer(AMessage moveans) {
        if (moveans.getComp(0).equals("movedto")) {
            agentX = Integer.parseInt(moveans.getComp(1));
            agentY = Integer.parseInt(moveans.getComp(2));

            System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")");
        }
    }

    /**
     * Send to the environment object the question:
     * "Does the detector sense something around(agentX,agentY) ?"
     *
     * @return return the answer given by the environment
     **/

    public AMessage DetectsAt() {
        AMessage msg, ans;

        msg = new AMessage("detected", Integer.toString(agentX),
                Integer.toString(agentY), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }


    /**
     * Process the answer obtained for the query "Detects at (x,y)?"
     * by adding the appropriate evidence clause to the formula
     *
     * @param ans message obtained to the query "Detects at (x,y)?".
     *            It will a message with four fields: detected  x y  [1,2,3]
     **/
    public void processDetectorSensorAnswer(AMessage ans) throws ContradictionException {
        if (ans.getComp(0).equals("detected")) {
            int x = Integer.parseInt(ans.getComp(1));
            int y = Integer.parseInt(ans.getComp(2));
            int sensorValue = Integer.parseInt(ans.getComp(3));

            VecInt evidence = new VecInt();
            if (sensorValue == 1) {
                System.out.println("WAR => adding evidence for detector 1 at : (" + x + "," + y + ")");
                evidence.insertFirst(coordToLineal(x, y, DetectorOffset1));
                solver.addClause(evidence);
            } else if (sensorValue == 2) {
                System.out.println("WAR => adding evidence for detector 2 at : (" + x + "," + y + ")");
                evidence.insertFirst(coordToLineal(x, y, DetectorOffset2));
                solver.addClause(evidence);
            } else if (sensorValue == 3) {
                System.out.println("WAR => adding evidence for detector 3 at : (" + x + "," + y + ")");
                evidence.insertFirst(coordToLineal(x, y, DetectorOffset3));
                solver.addClause(evidence);
            }
        }
    }

    /**
     * This function should add all the clauses stored in the list
     * futureToPast to the formula stored in solver.
     * Use the function addClause( VecInt ) to add each clause to the solver
     **/
    public void addLastFutureClausesToPastClauses() throws IOException, ContradictionException, TimeoutException {
        for (VecInt clause : futureToPast) {
            solver.addClause(clause);
        }

        futureToPast = new ArrayList<>();
    }


    /**
     * This function should check, using the future variables related
     * to possible positions of Treasure, whether it is a logical consequence
     * that Treasure is NOT at certain positions. This should be checked for all the
     * positions of the Treasure World.
     * The logical consequences obtained, should be then stored in the futureToPast list
     * but using the variables corresponding to the "past" variables of the same positions
     * <p>
     * An efficient version of this function should try to not add to the futureToPast
     * conclusions that were already added in previous steps, although this will not produce
     * any bad functioning in the reasoning process with the formula.
     *
     *  @throws ContradictionException
     *  @throws TimeoutException
     **/

    public void performInferenceQuestions() throws IOException, ContradictionException, TimeoutException {
        for (int x = 1; x <= WorldDim; x += 1) {
            for (int y = 1; y <= WorldDim; y += 1) {
                int linealIndex = coordToLineal(x, y, TreasureFutureOffset);
                // Get the same variable, but in the past subset
                int linealIndexPast = coordToLineal(x, y, TreasurePastOffset);

                VecInt variablePositive = new VecInt();
                variablePositive.insertFirst(linealIndex);



                // Check if the variable is already in the list
                if (!(solver.isSatisfiable(variablePositive))) {
                    VecInt concPast = new VecInt();
                    concPast.insertFirst(-(linealIndexPast));

                    futureToPast.add(concPast);
                    tfstate.set(x, y, "X");
                }
            }
        }
    }

    /**
     * This function builds the initial logical formula of the agent and stores it
     * into the solver object.
     *
     * @return returns the solver object where the formula has been stored
     *
     * totalNumVariables = 5 * WorldDim * WorldDim * (5 variables per cell)
     * Vars = { sensor1 in x,y (t) ,sensor2 in x,y (t) ,sensor3 in x,y (t) ,tr in x,y (t−1),tr in x,y (t+1) | (x,y) ∈ [1,n] × [1,n]}
     *
     *
     * @throws ContradictionException
     * @throws IOException
     * @throws TimeoutException
     * @throws UnsupportedEncodingException
     *
     *
     **/
    public ISolver buildGamma() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException {

        int totalNumVariables = WorldDim * WorldDim * 5;

        // You must set this variable to the total number of boolean variables
        // in your formula Gamma
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);

        actualLiteral = 1;

        noTreasureAtNextIfNoTreasureAtPrev();
        atLeastOneTreasure();
        noTreasureOutsideS1();
        noTreasureOutsideS2();
        noTreasureOutsideS3();

        return solver;
    }

    /**
     *
     * This function adds the clause of:
     *          ∀x,y ∈ [1, n] × [1, n] (¬tr in x,y (t−1) → ¬tr in x,y (t+1))
     * With this clause we add the restriction that past must be consistent with the future.
     *
     *  @throws ContradictionException
     *
     **/
    private void noTreasureAtNextIfNoTreasureAtPrev() throws ContradictionException {
        TreasurePastOffset = actualLiteral;
        TreasureFutureOffset = TreasurePastOffset + WorldLinealDim;

        for (int x = 1; x <= WorldDim; x++) {
            for (int y = 1; y <= WorldDim; y++) {
                int trtMinus1 = coordToLineal(x, y, TreasurePastOffset);
                int trtPlus1 = coordToLineal(x, y, TreasureFutureOffset);
                VecInt clause = new VecInt();
                // ¬tr in x,y (t−1) → ¬tr in x,y (t+1) == tr in x,y (t−1) ∨ ¬tr in x,y (t+1)
                clause.insertFirst(trtMinus1); // tr in x,y (t−1)
                clause.insertFirst(-trtPlus1); // ¬tr in x,y (t+1)
                solver.addClause(clause);
                actualLiteral += 2;
            }
        }
    }

    /**
     *
     * This function adds the clauses of::
     *      (tr in 1,1 (t−1) ∨ tr in 1,2 (t−1) ∨ ... ∨ tr in n,n (t−1))
     *      (tr in 1,1 (t+1) ∨ tr in 1,2 (t+1) ∨ ... ∨ tr in n,n (t+1))
     *
     * With this clause we add the restriction that there is at least one treasure in the world.
     *  @throws ContradictionException
     *
     **/
    private void atLeastOneTreasure() throws ContradictionException {
        VecInt clausePast = new VecInt();
        VecInt clauseFuture = new VecInt();
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = 1; j <= WorldDim; j++) {
                int literal1 = coordToLineal(i, j, TreasurePastOffset);
                int literal2 = coordToLineal(i, j, TreasureFutureOffset);
                // tr in 1,1 (t−1) ∨ tr in 1,2 (t−1) ∨ ... ∨ tr in n,n (t−1)
                clausePast.insertFirst(literal1);
                // tr in 1,1 (t+1) ∨ tr in 1,2 (t+1) ∨ ... ∨ tr in n,n (t+1)
                clauseFuture.insertFirst(literal2);
            }
        }
        solver.addClause(clausePast);
        solver.addClause(clauseFuture);
    }

    /**
     *
     * This functions adds the clauses of::
     *      ∀(x, y) ∈ [1, n] × [1, n], ∀(x′, y′)  not ∈ {{x, y - 1}, {x, y}, {x, y + 1}, {x - 1, y}, {x + 1, y}}
     *      (sensor1 in x,y (t) → ¬tr in x',y' (t+1) )
     *
     * There is no treasure in one of the five cells if the reading is 1.
     *
     * @throws ContradictionException
     *
     **/

    private void noTreasureOutsideS1() throws ContradictionException {
        DetectorOffset1 = actualLiteral;
        for (int x = 1; x <= WorldDim; x++) {
            for (int y = 1; y <= WorldDim; y++) {
                addS1Clauses(x, y);
                actualLiteral++;
            }

        }

    }
    /**
     * @param x x coordinate of the position to check
     * @param y coordinate of the position to check
     * @throws ContradictionException
     */
    private void addS1Clauses(int x, int y) throws ContradictionException {
        int[][] sensor = {{x, y - 1}, {x, y}, {x, y + 1}, {x - 1, y}, {x + 1, y}};
        for(int i = 1; i <= WorldDim; i++) {
            for(int j = 1; j <= WorldDim; j++) {
                int posS1_XY = coordToLineal(x, y, DetectorOffset1);
                if(notInSensor(i, j, sensor)) {
                    VecInt notOutsideS1 = new VecInt();
                    int treasurePos = coordToLineal(i, j, TreasureFutureOffset);
                    // sensor1 in x,y (t) → ¬tr in x',y' (t+1) == ¬sensor1 in x,y (t) ∨ ¬tr in x',y' (t+1)
                    notOutsideS1.insertFirst(-posS1_XY); // ¬sensor1 in x,y (t)
                    notOutsideS1.insertFirst(-treasurePos); // ¬tr in x',y' (t+1)
                    solver.addClause(notOutsideS1);
                }
            }
        }
    }

    /**
     *
     * This functions adds the clauses of::
     *      ∀(x, y) ∈ [1, n] × [1, n], ∀(x′, y′)  not ∈ {{x + 1, y + 1}, {x + 1, y - 1}, {x - 1, y - 1}, {x - 1, y + 1}}
     *      (sensor2 in x,y (t) → ¬tr in x',y' (t+1) )
     *
     * There is no treasure in one of the four cells if the reading is 2.
     *
     **/

    private void noTreasureOutsideS2() throws ContradictionException {
        DetectorOffset2 = actualLiteral;

        for (int x = 1; x <= WorldDim; x++) {
            for (int y = 1; y <= WorldDim; y++) {
                addS2Clauses(x, y);
                actualLiteral++;
            }
        }
    }

    /**
     * @param x coordinate of the position to check
     * @param y coordinate of the position to check
     * @throws ContradictionException
     */

    private void addS2Clauses(int x, int y) throws ContradictionException {
        int[][] sensor = {{x + 1, y + 1}, {x + 1, y - 1}, {x - 1, y - 1}, {x - 1, y + 1}};
        for(int i = 1; i <= WorldDim; i++) {
            for(int j = 1; j <= WorldDim; j++) {
                int posS2_XY = coordToLineal(x, y, DetectorOffset2);
                if(notInSensor(i, j, sensor)) {
                    VecInt notOutsideS2 = new VecInt();
                    int treasurePos = coordToLineal(i, j, TreasureFutureOffset);
                    // sensor2 in x,y (t) → ¬tr in x',y' (t+1) == ¬sensor2 in x,y (t) ∨ ¬tr in x',y' (t+1)
                    notOutsideS2.insertFirst(-posS2_XY); // ¬sensor2 in x,y (t)
                    notOutsideS2.insertFirst(-treasurePos); // ¬tr in x',y' (t+1)
                    solver.addClause(notOutsideS2);
                }
            }
        }
    }


    /**
     *
     * This functions adds the clauses of:
     *     ∀(x, y) ∈ [1, n] × [1, n], ∀(x′, y′) ∈ (x,y) U sensor1_positions(x,y) U sensor2_positions(x,y)
     *     (sensor3 in x,y (t) → ¬tr in x',y' (t+1))
     *
     *   There is not treasure in positions of sensor1 and positions of sensor2 if the reading is 3.
     *
     **/

    private void noTreasureOutsideS3() throws ContradictionException {
        DetectorOffset3 = actualLiteral;
        for(int x = 1; x <= WorldDim; x++) {
            for(int y = 1; y <= WorldDim; y++) {
                int posS3_XY = coordToLineal(x, y, DetectorOffset3);
                int[][] sensor = {{x, y - 1}, {x, y}, {x, y + 1}, {x - 1, y}, {x + 1, y}, {x + 1, y + 1}, {x + 1, y - 1}, {x - 1, y - 1}, {x - 1, y + 1}};
                for(int k = 0; k < sensor.length; k++) {
                    if(withinLimits(sensor[k][0], sensor[k][1])) {
                        // here we don't check if the position is in the sensor because we want to add the clauses that are in the sensor 1 and 2 positions
                        VecInt notInS3 = new VecInt();
                        int treasurePos = coordToLineal(sensor[k][0], sensor[k][1], TreasureFutureOffset);
            //      sensor3 in x,y (t) → ¬tr in x',y' (t+1) == ¬sensor3 in x,y (t) ∨ ¬tr in x',y' (t+1)
                        notInS3.insertFirst(-posS3_XY); // ¬sensor3 in x,y (t)
                        notInS3.insertFirst(-treasurePos); // ¬tr in x',y' (t+1)
                        solver.addClause(notInS3);
                    }
                }
            }
        }
    }

    /**
     * @AUXILIAR METHOD
     * This functions checks if a position is within the limits of the world (n x n) and also if given position belongs to the sensor set of positions.
     * @param i x coordinate of the position to check
     * @param j y coordinate of the position to check
     * @param sensor set of positions of the sensor
     **/
    private boolean notInSensor(int i, int j, int[][] sensor) {
        for(int k = 0; k < sensor.length; k++) {
            if(withinLimits(sensor[k][0], sensor[k][1])) {
                if (i == sensor[k][0] && j == sensor[k][1]) {
                    return false;
                }
            }
        }
        return true;
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

    /**
         * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
         * of variable that stores that information in the formula, using
         * offset as the initial index for that subset of position variables
         * (past and future position variables have different variables, so different
         * offset values)
         *
         *  @param x x coordinate of the position variable to encode
         *  @param y y coordinate of the position variable to encode
         *  @param offset initial value for the subset of position variables
         *         (past or future subset)
         *  @return the integer indentifer of the variable  b_[x,y] in the formula
        **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x-1) * WorldDim) + (y-1) + offset;
    }


    /**
     * Perform the inverse computation to the previous function.
     * That is, from the identifier t_[x,y] to the coordinates  (x,y)
     *  that it represents
     *
     * @param lineal identifier of the variable
     * @param offset offset associated with the subset of variables that
     *        lineal belongs to
     * @return array with x and y coordinates
    **/
    public int[] linealToCoord(int lineal, int offset)
    {
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = ((lineal-1) % WorldDim) + 1;
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }



}
