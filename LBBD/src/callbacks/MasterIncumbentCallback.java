package callbacks;

import java.io.IOException;
import java.util.ArrayList;

import models.Subproblem;
import bendersDecomposition.LogicBasedBenders;
import bendersDecomposition.MasterSolution;
import bendersDecomposition.User;
import helperClasses.FileManipulation;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;



public class MasterIncumbentCallback extends IloCplex.IncumbentCallback{
	public LogicBasedBenders bd;
	public boolean isInfeasible; //check that it subproblem is infeasible
	//public  double bestFeasibleSolutionObjective=-1;
	
	public MasterIncumbentCallback(LogicBasedBenders bd ){
		this.bd = bd;
		this.isInfeasible = false;
	}

	/**
	 * set the values based on decision variables
	 *
	 */
	public MasterSolution setResults ()
	{
		int capacity =0;
		MasterSolution solution = new MasterSolution(this.bd.master.U, this.bd.master.A, this.bd.master.J, this.bd.master.P.length);
		
		try {
			
			solution.objective =  this.getIncumbentObjValue();//this.getObjValue();
			
			//loop over functions
			for (int a= 0; a < solution.A; a++) {
				
				//loop over capacities
				for (int p=0; p<solution.P; p++)
				{
					//start by performing some rounding (just in case)
					solution.zapValues[a][p]=this.getIncumbentValue(this.bd.master.zap[a][p])>=0.9?1:0;										
					capacity+=solution.zapValues[a][p]*this.bd.master.P[p];					
				}				
				solution.pa[a] = capacity;
				capacity=0;
				
				solution.naValues[a] = this.getIncumbentValue(this.bd.master.na[a])>=0.9?1:0;
				
				for (int u=0;u<this.bd.master.U; u++)
				{
					solution.quaValues[u][a]=this.getIncumbentValue(this.bd.master.qua[u][a])>=0.9?1:0;
				}
			}
			
			for (int u= 0; u < solution.U; u++) {		
				for (int a= 0; a < solution.A; a++) {
					
					//this user can not be scheduled on this app-> do not create a variable
					if (this.bd.master.P_uaj.get(u).get(a)==null)
					{
						continue;
					}
					
					//create variables only for existing values in dprocess_uaj
					for (int j=0; j<this.bd.master.P_uaj.get(u).get(a).size(); j++)
					{
						solution.beta_uajValues[u][a][j] = this.getIncumbentValue(this.bd.master.beta_uaj[u][a][j])>=0.9?1:0;						
					}					
				}
			}
			
			
		} catch (UnknownObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return solution;
	}
	
	
	/**
	 * This method will initialize a subproblem per app 
	 * Do not add subproblems if using callbacks
	 */
	public Subproblem [] initializeSubProblemsBasedOnMaster (MasterSolution solution)
	{	
		//get list of users assigned to each app by the master
		ArrayList<ArrayList<Integer>> admittedUEsPerApp = solution.getAdmittedUEsPertApp();
		
		Subproblem [] subproblems = new Subproblem[solution.A];
		User ue=null;
		int ueIdInMaster =0;
		double processingTime =0;
		User [] admittUserPerApp =null;
		
		for(int a=0; a<solution.A; a++ )
		{
			admittUserPerApp = new User [admittedUEsPerApp.get(a).size()];
			for (int u=0; u<admittedUEsPerApp.get(a).size(); u++)
			{
				ueIdInMaster = admittedUEsPerApp.get(a).get(u);
				//processingTime = (int)Math.ceil((double)(this.data.mu_u[ueIdInMaster])/this.master.pa[a]);
				processingTime = Math.ceil((double)(this.bd.data.mu_u[ueIdInMaster])/solution.pa[a]);//wrong for incumbent
				ue = new User(ueIdInMaster, a, this.bd.data.theta_u[ueIdInMaster],this.bd.data.mu_u[ueIdInMaster], this.bd.data.sigma_ua[ueIdInMaster][a], processingTime);
				
				admittUserPerApp[u] = ue;
				
			}
			try {
				subproblems[a] = new Subproblem( a, solution.pa[a],admittUserPerApp,this.bd.iterations);
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		admittedUEsPerApp=null;
		return subproblems;
	
	
	}
	
	
	public void solveBenders(MasterSolution solution)
	{
		double incumbentSubProblemstartTime=0;
		double subproblemEndTime=0;
		Subproblem [] subproblems;
		 int totalSubProblemObjective=0;
		FileManipulation resultsFile,objectiveExecTime;
		
		this.isInfeasible = false;
		try {
			
			objectiveExecTime = new FileManipulation("results/BendersObjectiveTime.txt");
			
			resultsFile = new FileManipulation("testResults/BendersIncumbent.txt");
			resultsFile.writeInFile("-------------ITERATION:"+this.bd.iterations+"\n");
			resultsFile.writeInFile(solution.toString());
					
			//initialize and create incumbentSubProblems for the current iterations
			subproblems = this.initializeSubProblemsBasedOnMaster(solution);
			
			//start all incumbentSubProblems
			for (int i=0; i<subproblems.length; i++)
			{
				//if no users are assigned to this app no need to run the subproblem
				if (solution.naValues[subproblems[i].a]<0.9)
				{
					resultsFile.writeInFile(subproblems[i].toString());
					continue;
				}									
				
				//run subproblem								
				//start the this.incumbentSubProblems[i] thread that will call the run method
				incumbentSubProblemstartTime = System.currentTimeMillis();
				subproblems[i].buildILPModel();
				subproblems[i].start();
				
				subproblemEndTime = System.currentTimeMillis();
				subproblems[i].execTime = subproblemEndTime-incumbentSubProblemstartTime;
			}
			
			//once the join ends add cut.
			for (int i=0; i<subproblems.length; i++)
			{
				
				//if no users are assigned to this app no need to run the subproblem
				if (solution.naValues[subproblems[i].a]<0.9)
				{
					continue;
				}						
				
				//join all threads so we make sure that all threads are done execution before proceeding
				incumbentSubProblemstartTime = System.currentTimeMillis();
				
				subproblems[i].join();
				
				subproblemEndTime = System.currentTimeMillis();
				subproblems[i].execTime += subproblemEndTime-incumbentSubProblemstartTime;				
							
				totalSubProblemObjective+=subproblems[i].nbAdmittedUE;			
			
				//if the nb of users admitted by subproblem<those sent to it by the master (admitted by master on this app)
				if (subproblems[i].nbAdmittedUE<subproblems[i].assignedUEs.length )
				{	
					//evaluate subproblem result and generate and add cuts in main logic bender to prevent fatal error
					this.isInfeasible = true;
					//break; //if at least one is infeasible we break to add cuts and there we will check again if any other subproblem is infeasible
					
				}
				
				//write the subproblem
				resultsFile.writeInFile(subproblems[i].toString());				
			}
			
			
			if (this.isInfeasible)
			{
				
				objectiveExecTime.writeInFile(this.bd.iterations+"\t"+this.getObjValue()+"\t"+totalSubProblemObjective);
				objectiveExecTime.writeInFile("\t"+((double)(this.getObjValue()-totalSubProblemObjective)/this.getObjValue())*100);
				
				this.bd.incumbentSubProblems.add(subproblems);
			}
			/*else
			{
				this.bestFeasibleSolutionObjective = this.getIncumbentObjValue();
			}*/
			totalSubProblemObjective=0;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected void main() throws IloException {
		// TODO Auto-generated method stub
		//counter++;
		//check incumbent after at least 10 iterations for each call so we try not to check many feasible solutions for incumbent
		// if (counter%2==0)
		// {	
		
		/*if (this.bestFeasibleSolutionObjective!=-1 && this.getIncumbentObjValue() <this.bestFeasibleSolutionObjective)
		{
			this.reject();
		}
		else
		{*/
			 MasterSolution solution= this.setResults();
			this.solveBenders(solution);
		
			
			if (this.isInfeasible )
			{
				
				this.bd.incumbentMasterSolution.add(solution);
				this.bd.incumbentInfeasible = this.isInfeasible;
				this.isInfeasible = false;
				this.reject();//reject the incumbent solution so it is does not visit it again, can be forced only by adding a constraint that prevents going to incumbent again		
			
				this.abort();
			
				return;
			}
		
			//resetting to false so it does not reject feasible incumbent
			this.isInfeasible = false;
			this.bd.incumbentInfeasible = this.isInfeasible;
		//}
	}
	

	
}
