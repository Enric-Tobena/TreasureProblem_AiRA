package apryraz.tworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;

import org.sat4j.core.VecInt;
import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


import apryraz.tworld.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.*;

/**
*  Class for testing the TreasureFinder agent
**/
public class TreasureFinderTest {


    /**
     * This function should execute the next step of the agent, and the assert
     * whether the resulting state is equal to the targetState
     *
     * @param tAgent      TreasureFinder agent
     * @param targetState the state that should be equal to the resulting state of
     *                    the agent after performing the next step
     **/
    public void testMakeSimpleStep(TreasureFinder tAgent,
                                   TFState targetState) throws
            IOException, ContradictionException, TimeoutException {
        // Check (assert) whether the resulting state is equal to
        //  the targetState after performing action runNextStep with bAgent

        tAgent.runNextStep();
        assertTrue(targetState.equals(tAgent.getState()));


    }


    /**
     * Read an state from the current position of the file trough the
     * BufferedReader object
     *
     * @param br   BufferedReader object interface to the opened file of states
     * @param wDim dimension of the world
     **/
    public TFState readTargetStateFromFile(BufferedReader br, int wDim) throws
            IOException {
        TFState tfstate = new TFState(wDim);
        String row;
        String[] rowvalues;

        for (int i = wDim; i >= 1; i--) {
            row = br.readLine();
            rowvalues = row.split(" ");
            for (int j = 1; j <= wDim; j++) {
                tfstate.set(i, j, rowvalues[j - 1]);
            }
        }
        return tfstate;
    }

    /**
     * Load a sequence of states from a file, and return the list
     *
     * @param wDim       dimension of the world
     * @param numStates  num of states to read from the file
     * @param statesFile file name with sequence of target states, that should
     *                   be the resulting states after each movement in fileSteps
     * @return returns an ArrayList of TFState with the resulting list of states
     **/
    ArrayList<TFState> loadListOfTargetStates(int wDim, int numStates, String statesFile) {

        ArrayList<TFState> listOfStates = new ArrayList<TFState>(numStates);

        try {
            BufferedReader br = new BufferedReader(new FileReader(statesFile));
            String row;

            // steps = br.readLine();
            for (int s = 0; s < numStates; s++) {
                listOfStates.add(readTargetStateFromFile(br, wDim));
                // Read a blank line between states
                row = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => States file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(TreasureFinderTest.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }

        return listOfStates;
    }


    /**
     * This function should run the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps.
     *
     * @param wDim       the dimension of world
     * @param tX         x coordinate of Treasure position
     * @param tY         y coordinate of Treasure position
     * @param numSteps   num of steps to perform
     * @param fileSteps  file name with sequence of steps to perform
     * @param fileStates file name with sequence of target states, that should
     *                   be the resulting states after each movement in fileSteps
     **/
    public void testMakeSeqOfSteps(int wDim, int tX, int tY,
                                   int numSteps, String fileSteps,
                                   String fileStates)
            throws IOException, ContradictionException, TimeoutException {
        // You should make TreasureFinder and TreasureWorldEnv objects to  test.
        // Then load sequence of target states, load sequence of steps into the bAgent
        // and then test the sequence calling testMakeSimpleStep once for each step.
        TreasureFinder TAgent = new TreasureFinder(wDim);
        // load information about the World into the EnvAgent
        TreasureWorldEnv EnvAgent = new TreasureWorldEnv(wDim, tX, tY);
        // Load list of states
        ArrayList<TFState> seqOfStates;
        seqOfStates = loadListOfTargetStates(wDim, numSteps, fileStates);


        // Set environment agent and load list of steps into the agent
        TAgent.loadListOfSteps(numSteps, fileSteps);
        TAgent.setEnvironment(EnvAgent);

        // Test here the sequence of steps and check the resulting states with the
        // ones in seqOfStates

        for (int i = 0; i < numSteps; i++) {
            testMakeSimpleStep(TAgent, seqOfStates.get(i));

        }



    }

    /**
     * This is an example test. You must replicate this method for each different
     * test sequence, or use some kind of parametric tests with junit
     **/
    @Test
    public void TWorldTest1() throws
            IOException, ContradictionException, TimeoutException {

        //busca el file steps1.txt y el file states1.txt
        //en la carpeta tests

        var fileSteps = "src/test/tests/steps1.txt";
        if (!new java.io.File(fileSteps).exists()) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        }
        var fileStates = "src/test/tests/states1.txt";
        if (!new java.io.File(fileStates).exists()) {
            System.out.println("MSG.   => States file not found");
            exit(1);

            testMakeSeqOfSteps(6, 3, 3, 5, fileSteps, fileStates);
        }

    }
    @Test
    public void TWorldTest2() throws
            IOException, ContradictionException, TimeoutException {

        var fileSteps = "src/test/tests/steps2.txt";
        if (!new java.io.File(fileSteps).exists()) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        }
        var fileStates = "src/test/tests/states2.txt";
        if (!new java.io.File(fileStates).exists()) {
            System.out.println("MSG.   => States file not found");
            exit(1);

            testMakeSeqOfSteps(7, 4, 4, 6, fileSteps, fileStates);
        }
    }
    @Test
    public void TWorldTest3() throws
            IOException, ContradictionException, TimeoutException {

        var fileSteps = "src/test/tests/steps3.txt";
        if (!new java.io.File(fileSteps).exists()) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        }
        var fileStates = "src/test/tests/states3.txt";
        if (!new java.io.File(fileStates).exists()) {
            System.out.println("MSG.   => States file not found");
            exit(1);

            testMakeSeqOfSteps(8, 5, 4, 7, fileSteps, fileStates);
        }
    }
    @Test
    public void TWorldTest4() throws
            IOException, ContradictionException, TimeoutException {

        var fileSteps = "src/test/tests/steps4.txt";
        if (!new java.io.File(fileSteps).exists()) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        }
        var fileStates = "src/test/tests/states4.txt";
        if (!new java.io.File(fileStates).exists()) {
            System.out.println("MSG.   => States file not found");
            exit(1);

            testMakeSeqOfSteps(10, 6, 5, 7, fileSteps, fileStates);
        }
    }



    @Test
    public void testSolver() throws ContradictionException, TimeoutException {
        ISolver solver;
        int totalNumVariables;

        totalNumVariables = 2;
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);

        // add a simple implication: 1 -> 2
        VecInt implication = new VecInt();
        implication.insertFirst(2);
        implication.insertFirst(-1);

        solver.addClause(implication);

        checkImplicationSatisfiability(solver, implication);
        checkImplicationUnsatisifiability(solver, implication);
    }

    public void checkImplicationSatisfiability(ISolver solver, VecInt implication) throws TimeoutException {
        // case 1: (-1 or 2) and 1
        VecInt toPerformInference1 = new VecInt();
        toPerformInference1.insertFirst(-(implication.get(0)));
        Assert.assertTrue(solver.isSatisfiable(toPerformInference1));

        // case 2: (-1 or 2) and -2
        VecInt toPerformInference2 = new VecInt();
        toPerformInference2.insertFirst(-(implication.get(1)));
        Assert.assertTrue(solver.isSatisfiable(toPerformInference2));

        // case 3: (-1 or 2) and -1
        VecInt toPerformInference3 = new VecInt();
        toPerformInference3.insertFirst((implication.get(0)));
        Assert.assertTrue(solver.isSatisfiable(toPerformInference3));

        // case 4: (-1 or 2) and 2
        VecInt toPerformInference4 = new VecInt();
        toPerformInference4.insertFirst((implication.get(1)));
        Assert.assertTrue(solver.isSatisfiable(toPerformInference4));

        // case 5: (-1 or 2) and (-1 and 2)
        VecInt toPerformInference5 = new VecInt();
        toPerformInference5.insertFirst((implication.get(1)));
        toPerformInference5.insertFirst((implication.get(0)));
        Assert.assertTrue(solver.isSatisfiable(toPerformInference5));

    }
    public void checkImplicationUnsatisifiability(ISolver solver, VecInt implication) throws TimeoutException {
        // case 4: (-1 or 2) and (1 and -2)
        VecInt toPerformInference = new VecInt();
        toPerformInference.insertFirst(-(implication.get(0)));
        toPerformInference.insertFirst(-(implication.get(1)));

        Assert.assertFalse(solver.isSatisfiable(toPerformInference));
    }



}
