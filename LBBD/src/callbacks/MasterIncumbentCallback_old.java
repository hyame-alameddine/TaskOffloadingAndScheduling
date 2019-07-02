package callbacks;

import java.io.IOException;

import models.Subproblem;
import bendersDecomposition.LogicBasedBenders;
import helperClasses.FileManipulation;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;



public class MasterIncumbentCallback_old extends IloCplex.IncumbentCallback{
	public LogicBasedBenders bd;
	public boolean isInfeasible; //check that it subproblem is infeasible
	
	public MasterIncumbentCallback_old(LogicBasedBenders bd ){
		this.bd = bd;
		this.isInfeasible = false;
	}

	/**
	 * set the values based on decision variables
	 * NOT NEEDED
	 */
	public void setResults ()
	{
		int capacity =0;
		
		try {
			
			this.bd.master.masterObjectiveValue = (int) this.getObjValue();
			
			//loop over functions
			for (int a= 0; a < this.bd.master.A; a++) {
				
				//loop over capacities
				for (int p=0; p<this.bd.master.P.length; p++)
				{
					//start by performing some rounding (just in case)
					this.bd.master.zapValues[a][p]=this.getValue(this.bd.master.zap[a][p])>=0.9?1:0;										
					capacity+=this.bd.master.zapValues[a][p]*this.bd.master.P[p];					
				}				
				this.bd.master.pa[a] = capacity;
				capacity=0;
				
				this.bd.master.naValues[a] = this.getValue(this.bd.master.na[a])>=0.9?1:0;
				
				for (int u=0;u<this.bd.master.U; u++)
				{
					this.bd.master.quaValues[u][a]=this.getValue(this.bd.master.qua[u][a])>=0.9?1:0;
				}
			}
			
			for (int u= 0; u < this.bd.master.U; u++) {		
				for (int a= 0; a < this.bd.master.A; a++) {
					
					//this user can not be scheduled on this app-> do not create a variable
					if (this.bd.master.P_uaj.get(u).get(a)==null)
					{
						continue;
					}
					
					//create variables only for existing values in dprocess_uaj
					for (int j=0; j<this.bd.master.P_uaj.get(u).get(a).size(); j++)
					{
						this.bd.master.beta_uajValues[u][a][j] = this.getValue(this.bd.master.beta_uaj[u][a][j])>=0.9?1:0;						
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
	}
	
	public void solveBenders()
	{
		double incumbentSubProblemstartTime=0;
		double subproblemEndTime=0;
		Subproblem [] subproblems;
	
		FileManipulation resultsFile;
		try {
			
			resultsFile = new FileManipulation("testResults/BendersIncumbent.txt");
			resultsFile.writeInFile("-------------ITERATION:"+this.bd.iterations+"\n");
			
					
			//initialize and create incumbentSubProblems for the current iterations
			subproblems = this.bd.initializeSubProblemsBasedOnMaster();
			
			//start all incumbentSubProblems
			for (int i=0; i<subproblems.length; i++)
			{
				//if no users are assigned to this app no need to run the subproblem
				if (this.getValue(this.bd.master.na[subproblems[i].a])==0)
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
				if (this.getValue(this.bd.master.na[subproblems[i].a])==0)
				{
					continue;
				}						
				
				//join all threads so we make sure that all threads are done execution before proceeding
				incumbentSubProblemstartTime = System.currentTimeMillis();
				
				subproblems[i].join();
				
				subproblemEndTime = System.currentTimeMillis();
				subproblems[i].execTime += subproblemEndTime-incumbentSubProblemstartTime;				
									
				//if the nb of users admitted by subproblem<those sent to it by the master (admitted by master on this app)
				if (subproblems[i].nbAdmittedUE<subproblems[i].assignedUEs.length )
				{	
					//evaluate subproblem result and generate and add cuts in main logic bender to prevent fatal error
					this.isInfeasible = true;
					break; //if at least one is infeasible we break to add cuts and there we will check again if any other subproblem is infeasible
					
				}
				
				//write the subproblem
				resultsFile.writeInFile(subproblems[i].toString());					
			}
		
			if (this.isInfeasible)
			{
				this.bd.incumbentSubProblems.add(subproblems);
			}
			
			
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
		this.setResults();
		this.solveBenders();
	
		
		if (this.isInfeasible )
		{
			System.out.println("REJECTING ABORTING incumbent");
		
			this.bd.incumbentInfeasible = this.isInfeasible;
			this.isInfeasible = false;
			this.reject();//reject the incumbent solution so it is does not visit it again, can be forced only by adding a constraint that prevents going to incumbent again		
		
			this.abort();
		
			return;
		}
		System.out.println("INCUMBENT FEASIBLE");
		//resetting to false so it does not reject feasible incumbent
		this.isInfeasible = false;
	}
	

	
}
