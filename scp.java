
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.nary.automata.FA.FiniteAutomaton;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Final {
    public static void main(String[] args) {
        ShiftReader shiftReader = new ShiftReader("data/assign/shift0.txt");
        int numNurses = shiftReader.getNumNurses();
        int numShifts = shiftReader.getNumShifts();
        int numSkills = shiftReader.getNumSkills();
        int minNurses= shiftReader.getMinNurses(); //minimum nurses on each shift
        int minShifts = shiftReader.getMinShifts(); //minimum shifts per nurse
        int minBreakLength = shiftReader.getBreakLength();
        int minBreakPeriod = shiftReader.getBreakPeriod();
        int[][] nurseSkills = shiftReader.getNurseSkills();
        int[][] shiftSkills = shiftReader.getShiftSkills();


        //create two arrays to represent  A skills and B skills
        int[] aSkills = new int[numNurses];
        int[] bSkills = new int[numNurses];
        int [] requiredSkillsA = new int[numShifts];
        int [] requiredSkillsB = new int[numShifts];
        for (int nurse = 0; nurse < numNurses; nurse++) {
            aSkills[nurse] = nurseSkills[nurse][0];
            bSkills[nurse] = nurseSkills[nurse][1];
        }


        //find required skills for each shift
        for (int shift = 0; shift < numShifts; shift++) {
            requiredSkillsA[shift] = shiftSkills[shift][0];
            requiredSkillsB[shift] = shiftSkills[shift][1];
        }


        //create Model instance
        Model model = new Model("(work schedule");
        //array of total nurses for each shift, according to each shift requirement,
        IntVar[] totalNursesForEachShift = model.intVarArray("nurses", numShifts, 1, numNurses);

        //array of total shift for each shift, according to each shift requirement,
        IntVar[] totalShiftsForEachNurse = model.intVarArray("shifts", numNurses, 1, numShifts);

        //list of number of nurse with A skills in each shift
        IntVar[] skillsAForEachShift = model
                .intVarArray("shifts A skills", numShifts, 0, numSkills);
        //list of number of nurse with B skills in each shift
        IntVar[] skillsBForEachShift = model
                .intVarArray("shifts A skills", numShifts, 0, numSkills);

        //matrix for solution
        IntVar[][] assignNurseToShift= model.intVarMatrix("solution", numShifts , numNurses, 0, 1);
        IntVar[][] assignShiftToNurse = ArrayUtils.transpose(assignNurseToShift);

        for (int shift = 0; shift < numShifts; shift++) {
            model.scalar(assignNurseToShift[shift], aSkills, "=", skillsAForEachShift[shift]).post();
            model.scalar(assignNurseToShift[shift], bSkills, "=", skillsBForEachShift[shift]).post();
            model.sum(assignNurseToShift[shift], "=", totalNursesForEachShift[shift]).post();

            if (requiredSkillsA[shift] >= 1 && requiredSkillsB[shift] == 0) {
                model.sum(assignNurseToShift[shift], "=", skillsAForEachShift[shift]).post();
            } else if (requiredSkillsA[shift] == 0 && requiredSkillsB[shift] >=1) {
                model.sum(assignNurseToShift[shift], "=", skillsBForEachShift[shift]).post();
            }else {
                model.arithm(skillsAForEachShift[shift], "=", skillsBForEachShift[shift]).post();
            }
        }

        for( int nurse = 0; nurse < numNurses; nurse++) {
            model.regular(assignShiftToNurse[nurse],new FiniteAutomaton("(1?)(00*1)*0*")).post();
            model.regular(assignShiftToNurse[nurse],new FiniteAutomaton("(0|1)*00(0|1)*")).post();

            //model.regular(assignShiftToNurse[nurse],new FiniteAutomaton("(1?)(000*1)*0*")).post();

            model.sum(assignShiftToNurse[nurse], "=", totalShiftsForEachNurse[nurse]).post();
            model.sum(assignShiftToNurse[nurse], ">=", numShifts/numNurses).post();

        }
        //model.sum(totalShiftsForEachNurse, "=", numShifts).post();
        IntVar sumTotalSifts = model.intVar("sum_total", numShifts, numShifts*3 );
        model.sum(totalNursesForEachShift, "=", sumTotalSifts).post();
        model.setObjective(Model.MINIMIZE, sumTotalSifts);




        /**
         for (int shift = 0; shift < numShifts; shift++) {
         //int requiredSkillA = ArequiredSkills[shift];
         //int requiredSkillB = BrequiredSkills[shift];
         //System.out.println("required skills : "+requiredSkillA + "," + requiredSkillB);
         //int numASkills = Arrays.stream(aSkills).map(i -> i * requiredSkillA).sum();
         //System.out.println("a "+numASkills);
         //int numBSkills = Arrays.stream(bSkills).map(i -> i * requiredSkillB).sum();
         //System.out.println("b "+numBSkills)

         sumUp = model.intVar(0);
         scaleWithA = model.intVar(1);
         scaleWithB = model.intVar(1);

         for (int nurse = 0; nurse < numNurses; nurse++) {
         sumUp = sumUp.add(assignNurseToShift[shift][nurse]).intVar();
         scaleWithA = scaleWithA.add(assignNurseToShift[shift][nurse].mul(aSkills[nurse]).intVar()).intVar();
         scaleWithB = scaleWithB.add(assignNurseToShift[shift][nurse].mul(bSkills[nurse]).intVar()).intVar();
         }

         if (ArequiredSkills[shift] == 1 && BrequiredSkills[shift] == 0) {
         model.scalar(assignNurseToShift[shift], aSkills, "=", sumUp).post();
         } else if (ArequiredSkills[shift] == 0 && BrequiredSkills[shift] ==1) {
         model.scalar(assignNurseToShift[shift], bSkills, "=", sumUp).post();
         }else {
         model.and(scaleWithA.eq(scaleWithB).boolVar(), scaleWithB.eq(scaleWithA).boolVar()).post();
         }
         }**/



        //model.setObjective(Model.MINIMIZE, shiftsPerNurse);

        Solver solver = model.getSolver();
        while(solver.solve()) {
            //System.out.println("sum up is : " +sumUp.getValue());
            ///**
             System.out.println("************solution" + solver.getSolutionCount() + "***********" );
             for (int shift = 0; shift < numShifts; shift++) {
             System.out.print("\tS" + shift );
             }
            System.out.print("\t__total__");
            System.out.println();
             for (int nurse = 0; nurse < numNurses; nurse++) {
                 System.out.print("N" + nurse);
                 for (int shift = 0; shift < numShifts; shift++) {
                     System.out.print("\t" + assignShiftToNurse[nurse][shift].getValue());
                 }
                 System.out.print("\t\t"+totalShiftsForEachNurse[nurse].getValue());
                 System.out.println();
             }
             System.out.println("\n");
             //**/

            /**
            System.out.println("************solution" + solver.getSolutionCount() + "***********" );
            System.out.print("\t   ");
            for (int shift = 0; shift < numNurses; shift++) {
                System.out.print("\tNR" + shift );
            }
            System.out.print("\t\t\tA_skills");
            System.out.print("\tB _skills");
            System.out.print("\ttotal_nurse");

            System.out.println();
            for (int shift = 0; shift < numShifts; shift++) {
                System.out.print("S" + shift+ "\t" );
                for (int nurse = 0; nurse < numNurses; nurse++) {
                    System.out.print("\t " +assignNurseToShift[shift][nurse].getValue());
                }
                System.out.print("\t\t\t\t" +skillsAForEachShift[shift].getValue());
                System.out.print("\t\t\t" +skillsBForEachShift[shift].getValue());
                System.out.print("\t\t\t" +totalNursesForEachShift[shift].getValue());
                System.out.println();

            }
            System.out.println("-------------------------------");
            System.out.print("total");
            for (int nurse = 0; nurse < numNurses; nurse++) {
                System.out.print("\t"+totalShiftsForEachNurse[nurse].getValue());
            }
            System.out.println("\n\n");

            **/
        }
        solver.printStatistics();

    }
}



