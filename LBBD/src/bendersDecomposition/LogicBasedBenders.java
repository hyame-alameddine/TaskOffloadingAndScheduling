package bendersDecomposition;

import generalClasses.DataGeneration;
import helperClasses.FileManipulation;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.io.IOException;
import java.util.ArrayList;

import callbacks.MasterIncumbentCallback;
import models.Master;
import models.Subproblem;

public class LogicBasedBenders {

	public	DataGeneration data;
	public Master master;
	public ArrayList<Double>masterObjectivePerIteration;
	public ArrayList<Subproblem[]>subProblems;//sub-problems per iteration
	public ArrayList<Subproblem[]>incumbentSubProblems;//sub-problems  when using incumbent callback for each iterations, many incumbent solution per iteration
	public ArrayList<MasterSolution>incumbentMasterSolution;
	public int iterations;//holds the total iterations and updated at each iteration
	public int totalSubProblemObjective;
	public boolean incumbentInfeasible;
	
	public LogicBasedBenders( DataGeneration data)
	{
		this.data = data;
		try {
			
			this.master = new Master(this.data);
			//set subproblem to null. It will be initialized in initializePricingBasedOnMaster()
			this.subProblems = new ArrayList<Subproblem[]>();			
			this.incumbentSubProblems = new ArrayList<Subproblem[]>();
			this.incumbentMasterSolution = new ArrayList<MasterSolution>();
			this.masterObjectivePerIteration = new ArrayList<Double>();
			this.iterations = -1;
			this.totalSubProblemObjective =0;
			this.incumbentInfeasible = false;
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	/**
	 * This method will initialize a subproblem per app 
	 * Do not add subproblems if using callbacks
	 */
	public Subproblem [] initializeSubProblemsBasedOnMaster ()
	{	
		//get list of users assigned to each app by the master
		ArrayList<ArrayList<Integer>> admittedUEsPerApp = this.master.getAdmittedUEsPertApp();
		
		Subproblem [] subproblems = new Subproblem[this.data.A];
		User ue=null;
		int ueIdInMaster =0;
		double processingTime =0;
		User [] admittUserPerApp =null;
		
		for(int a=0; a<this.data.A; a++ )
		{
			admittUserPerApp = new User [admittedUEsPerApp.get(a).size()];
			for (int u=0; u<admittedUEsPerApp.get(a).size(); u++)
			{
				ueIdInMaster = admittedUEsPerApp.get(a).get(u);
				//processingTime = (int)Math.ceil((double)(this.data.mu_u[ueIdInMaster])/this.master.pa[a]);
				processingTime = Math.ceil((double)(this.data.mu_u[ueIdInMaster])/this.master.pa[a]);//wrong for incumbent
				ue = new User(ueIdInMaster, a, this.data.theta_u[ueIdInMaster],this.data.mu_u[ueIdInMaster], this.data.sigma_ua[ueIdInMaster][a], processingTime);
				
				admittUserPerApp[u] = ue;
				
			}
			try {
				subproblems[a] = new Subproblem( a, this.master.pa[a],admittUserPerApp,this.iterations);
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		admittedUEsPerApp=null;
		return subproblems;
	
	
	}
	
	/**
	 * this method contains the logic of benders
	 * Use threading for the subproblem
	 */
	public double[] solve(boolean useMasterIncumbentCallback)
	{
		boolean stop = false;
		FileManipulation resultsFile, objectiveExecTime;
		double []results = new double [5];
		
		
		double startTime=0;
		double endTime =0;	
		double masterStartTime=0;
		double masterEndTime=0;
		double subproblemStartTime=0;
		double subproblemEndTime=0;
		double subproblemsSTime=0;
		MasterIncumbentCallback masterCallBack;
		try {
			objectiveExecTime = new FileManipulation("results/BendersObjectiveTime.txt");
			objectiveExecTime.writeInFile("Iteration\tMasterObjective\tTotalSubProblemObjective\tGap\tMasterRunTime\tSubproblemsRunTime\tTotalRunTimeTillThisIteration\n");
			startTime = System.currentTimeMillis();
			
			this.master.buildILPModel();
			
			//Tell cplex to stop running the pricing at a feasible solution with a certain gap to gain time
			if (useMasterIncumbentCallback)
			{	
				masterCallBack = new MasterIncumbentCallback(this);
				this.master.cplex.use(masterCallBack);
			}
			
			while (!stop )//&& iterations!=0
			{			
				stop = true;
				
				this.iterations++;
				//resultsFile = new FileManipulation("testResults/Benders_"+this.iterations+".txt");
				resultsFile = new FileManipulation("testResults/Benders-detailed.txt");
				
				masterStartTime = System.currentTimeMillis();
				
				this.master.runILPModel(resultsFile);
				
				masterEndTime = System.currentTimeMillis();
				
				this.masterObjectivePerIteration.add(this.master.masterObjectiveValue);
				
				
				 if (useMasterIncumbentCallback &&this.incumbentInfeasible )// 
				 {
					 this.addIncumbentCut();	
					 
					 objectiveExecTime.writeInFile("\t"+(int)(masterEndTime-masterStartTime));
					objectiveExecTime.writeInFile("\tx");				
					objectiveExecTime.writeInFile("\t"+(int)(System.currentTimeMillis()-startTime)+"\n");
					
					 this.subProblems.add(new Subproblem[0]);// at each iteration add an empty array of subproblems
					 stop = false;
					 this.incumbentInfeasible = false;//reset
					 
					 continue;				
					
				 }		
				
				 subproblemsSTime = System.currentTimeMillis();
				//initialize and create subproblems for the current iterations with optimal master
				this.subProblems.add(this.initializeSubProblemsBasedOnMaster());
				
				//start all subproblems
				for (int i=0; i<this.subProblems.get(this.iterations).length; i++)
				{
					//if no users are assigned to this app no need to run the subproblem
					if (this.master.naValues[this.subProblems.get(this.iterations)[i].a]<0.9)
					{
						resultsFile.writeInFile(this.subProblems.get(this.iterations)[i].toString());
						continue;
					}									
					
					//run subproblem								
					//start the this.subProblems[i] thread that will call the run method
					subproblemStartTime = System.currentTimeMillis();
					
					//run pricing
					this.subProblems.get(iterations)[i].buildILPModel();
					
					this.subProblems.get(this.iterations)[i].start();
					
					subproblemEndTime = System.currentTimeMillis();
					this.subProblems.get(this.iterations)[i].execTime = subproblemEndTime-subproblemStartTime;
				}
				
				//once the join ends add cut.
				for (int i=0; i<this.subProblems.get(this.iterations).length; i++)
				{
					//if no users are assigned to this app no need to run the subproblem
					if (this.master.naValues[this.subProblems.get(this.iterations)[i].a]<0.9)
					{
						continue;
					}						
					
					//join all threads so we make sure that all threads are done execution before proceeding
					subproblemStartTime = System.currentTimeMillis();
					
					this.subProblems.get(this.iterations)[i].join();
					
					subproblemEndTime = System.currentTimeMillis();
					this.subProblems.get(this.iterations)[i].execTime += subproblemEndTime-subproblemStartTime;				
					this.totalSubProblemObjective+=this.subProblems.get(this.iterations)[i].nbAdmittedUE;
					//if the nb of users admitted by subproblem<those sent to it by the master (admitted by master on this app)
					if (this.subProblems.get(this.iterations)[i].nbAdmittedUE<this.subProblems.get(this.iterations)[i].assignedUEs.length )
					{	
						//evaluate subproblem result and generate and add cuts
						this.cutPreventAssigningLessPa(this.subProblems.get(this.iterations)[i]);
						
						stop = false;			
						
					}
					this.subProblems.get(this.iterations)[i].cplex.end();
				}
				
				objectiveExecTime.writeInFile(this.iterations+"\t"+this.master.masterObjectiveValue+"\t"+this.totalSubProblemObjective);
				objectiveExecTime.writeInFile("\t"+((double)(this.master.masterObjectiveValue-this.totalSubProblemObjective)/this.master.masterObjectiveValue)*100);
				objectiveExecTime.writeInFile("\t"+(int)(masterEndTime-masterStartTime));
				objectiveExecTime.writeInFile("\t"+(int)(System.currentTimeMillis()-subproblemsSTime));				
				objectiveExecTime.writeInFile("\t"+(int)(System.currentTimeMillis()-startTime)+"\n");
				
				if (((double)(this.master.masterObjectiveValue-this.totalSubProblemObjective)/this.master.masterObjectiveValue)*100<10)
				{
					stop=true;
				}
				this.totalSubProblemObjective=0;
			}
		
			endTime = System.currentTimeMillis();
			//report results
			results[0] = this.masterObjectivePerIteration.get(this.iterations);
			results[1] = (int)(endTime-startTime);
			results[2] = this.getTotalNbUsedApplication();
			results[3] = this.getTotalAssignedPa();
			results[4] = ++this.iterations;//since we start by 0			
						
						

			this.master.cplex.end();
			this.subProblems = null;
			this.data = null;			
			
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * Adds the following cut to tell the master to change pa if you want to admit the rejected task
	 * sum_u {admitted+1 of rejected by subproblem} beta_uaj<= admitted Users by subproblem - 1 for all a, for all rejected users
	 * 
	 * @param subproblem
	 */
	public void cutChangePa(Subproblem subproblem)
	{
		IloNumExpr sumubetauaj_reject;
		IloNumExpr sumubetauaj_admitted;
		int userIDInMaster =0;
		try {
			
						
			sumubetauaj_admitted= this.master.cplex.numExpr();
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{	
				userIDInMaster = subproblem.assignedUEs[u].idInMaster;
				//this user can not be scheduled on this app-> beta_uaj not created a variable, or not admitted
				if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a)==null || !subproblem.assignedUEs[u].admitted)
				{
					continue;
				}				
				
				//sum over admitted users by subproblem
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.master.P_uaj.get(userIDInMaster).get(subproblem.a).size(); j++)
				{
					if (this.master.beta_uajValues[userIDInMaster][subproblem.a][j]>0.9)
					sumubetauaj_admitted = this.master.cplex.sum(sumubetauaj_admitted,this.master.beta_uaj[userIDInMaster][subproblem.a][j]);
					
				}					
			}
			
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{	
				userIDInMaster = subproblem.assignedUEs[u].idInMaster;
				
				//this user can not be scheduled on this app-> beta_uaj not created a variable OR user admitted - do not add cut
				if (subproblem.assignedUEs[u].admitted || this.master.P_uaj.get(userIDInMaster).get(subproblem.a)==null)
				{
					continue;
				}

				sumubetauaj_reject =  this.master.cplex.numExpr();
				
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.master.P_uaj.get(userIDInMaster).get(subproblem.a).size(); j++)
				{
					if (this.master.beta_uajValues[userIDInMaster][subproblem.a][j]>0.9)
					sumubetauaj_reject = this.master.cplex.sum(sumubetauaj_reject, this.master.beta_uaj[userIDInMaster][subproblem.a][j]);
				}
				
				//add cut for each rejected task
				this.master.cplex.addLe (this.master.cplex.sum(sumubetauaj_admitted, sumubetauaj_reject), subproblem.nbAdmittedUE, "cut1: Change pa if you want to admit ue "+userIDInMaster);
			}
				
				
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	/**
	 * Adds the following cut to tell the master to change pa if you want to admit the rejected task
	 * sum_u {admitted+1 of rejected by subproblem} beta_uaj<= admitted Users by subproblem - 1 for all a, for all rejected users
	 * 
	 * @param subproblem
	 */
	public void cutPreventAssigningLessPa(Subproblem subproblem)
	{
		IloNumExpr sumubetauaj_reject;
		IloNumExpr sumubetauaj_admitted;
		int userIDInMaster =0;
		try {
			
						
			sumubetauaj_admitted= this.master.cplex.numExpr();
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{	
				userIDInMaster = subproblem.assignedUEs[u].idInMaster;
				//this user can not be scheduled on this app-> beta_uaj not created a variable, or not admitted
				if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a)==null || !subproblem.assignedUEs[u].admitted)
				{
					continue;
				}				
				
				//sum over admitted users by subproblem
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.master.P_uaj.get(userIDInMaster).get(subproblem.a).size(); j++)
				{
					if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a).get(j)<=subproblem.pa)
					sumubetauaj_admitted = this.master.cplex.sum(sumubetauaj_admitted,this.master.beta_uaj[userIDInMaster][subproblem.a][j]);
					
				}					
			}
			
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{	
				userIDInMaster = subproblem.assignedUEs[u].idInMaster;
				
				//this user can not be scheduled on this app-> beta_uaj not created a variable OR user admitted - do not add cut
				if (subproblem.assignedUEs[u].admitted || this.master.P_uaj.get(userIDInMaster).get(subproblem.a)==null)
				{
					continue;
				}

				sumubetauaj_reject =  this.master.cplex.numExpr();
				
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.master.P_uaj.get(userIDInMaster).get(subproblem.a).size(); j++)
				{
					if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a).get(j)<=subproblem.pa)
					sumubetauaj_reject = this.master.cplex.sum(sumubetauaj_reject, this.master.beta_uaj[userIDInMaster][subproblem.a][j]);
				}
				
				//add cut for each rejected task
				this.master.cplex.addLe (this.master.cplex.sum(sumubetauaj_admitted, sumubetauaj_reject), subproblem.nbAdmittedUE, "cut1: Change pa if you want to admit ue "+userIDInMaster);
				
			}
				
				
			
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * this method prevents assignment of stricter tasks
	 * @param subproblem
	 */
	public void cutStricterThanRejected(Subproblem subproblem)
	{
		IloNumExpr sumubetauaj_reject;
		IloNumExpr sumubetauaj_admitted;
		ArrayList<Integer> stricterUEs ;
		int userIDInMaster =0;
		try {
			
			if (subproblem.assignedUEs.length ==1 && subproblem.nbAdmittedUE ==1)
			{
				return;
			}
						
			sumubetauaj_admitted= this.master.cplex.numExpr();
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{	
				userIDInMaster = subproblem.assignedUEs[u].idInMaster;
				//this user can not be scheduled on this app-> beta_uaj not created a variable, or not admitted
				if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a)==null)
				{
					continue;
				}
				
				if (subproblem.assignedUEs[u].admitted)
				{
					//sum over admitted users by subproblem
					//create variables only for existing values in dprocess_uaj
					for (int j=0; j<this.master.P_uaj.get(userIDInMaster).get(subproblem.a).size(); j++)
					{
						if (this.master.P_uaj.get(userIDInMaster).get(subproblem.a).get(j)<=this.master.pa[subproblem.a])
						{System.out.println("aaa");
							sumubetauaj_admitted = this.master.cplex.sum(sumubetauaj_admitted,this.master.beta_uaj[userIDInMaster][subproblem.a][j]);
						}
						
					}	
				}				
			}
			
			//loop over assigned UE by master per application
			for (int u=0; u<subproblem.assignedUEs.length; u++)
			{
				//if rejected get stricter UE
				if (!subproblem.assignedUEs[u].admitted)
				{	
					userIDInMaster = subproblem.assignedUEs[u].idInMaster;
					
					stricterUEs = this.getStricterUEs(subproblem.assignedUEs[u], subproblem);
					System.out.println("stricterUEs: "+stricterUEs+" than UE "+subproblem.assignedUEs[u].idInMaster+" on app "+subproblem.a);
					//no stricter tasks that can be assigned to same app can be found
					if (stricterUEs.size()==0)
					{
						continue;
					}
					
					for (int i=0; i<stricterUEs.size(); i++)
					{
						sumubetauaj_reject =  this.master.cplex.numExpr();
						//create variables only for existing values in dprocess_uaj
						for (int j=0; j<this.master.P_uaj.get(stricterUEs.get(i)).get(subproblem.a).size(); j++)
						{
							if (this.master.P_uaj.get(stricterUEs.get(i)).get(subproblem.a).get(j)<=this.master.pa[subproblem.a])
							{System.out.println("fff");
								sumubetauaj_reject = this.master.cplex.sum(sumubetauaj_reject,this.master.beta_uaj[stricterUEs.get(i)][subproblem.a][j]);
							}
							
						}
						//add cut for each rejected task
						this.master.cplex.addLe (this.master.cplex.sum(sumubetauaj_admitted, sumubetauaj_reject), subproblem.nbAdmittedUE, "cut2: Change pa if stricter than rejected ue "+userIDInMaster);
					
						
					}
				}
								
			}
			
		}catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method returns an array with stricter UE than the rejected UE if assigned on subproblem.a
	 * Stricter is defined : arrival+edge>= arriva+edge of rejected  and pmina <=pa
	 * @param rejectedUe
	 * @param subproblem
	 * @return array of ue in master that are stricter than rejected ue
	 */
	public ArrayList<Integer>getStricterUEs(User rejectedUe, Subproblem subproblem)
	{
		ArrayList<Integer> stricterUEs = new ArrayList<Integer>();
		for(int u=0; u<this.master.U; u++)
		{
			//those are already taken care off
			if (subproblem.isAssigned(u))
			{
				continue;
			}
			
			//this user can not be scheduled on this app-> beta_uaj not created a variable, or not admitted
			if (this.master.P_uaj.get(u).get(subproblem.a)==null)
			{
				continue;
			}
			
			//does not have stricter arrival
			if (this.master.sigma_ua[u][subproblem.a]< rejectedUe.sigma)
			{
				continue;
			}
			
			//if it has stricter arrival, check if pmina<pa -- considers that P_uaj is sorted in ascending order
			if (this.master.P_uaj.get(u).get(subproblem.a).get(0)<=subproblem.pa)
			{
				stricterUEs.add(u);
			}
		}
		
		return stricterUEs;
	}
	
	
	/**
	 * this will add cut for the subproblem incumbent
	 * 
	 * 
	 */
	public void addIncumbentCut()
	{
		
		//all subproblems feasible - no cut to add	
		if (this.incumbentSubProblems.size()==0)
		{
			return ;
		}
		System.out.println("Incumbent subproblems "+this.incumbentSubProblems.size());
		//loop over incumbent solutions - many possible incumbent
		for (int j=0; j<this.incumbentSubProblems.size(); j++)
		{		
				//loop over subproblems in each incumbent solution
				for (int i=0; i<this.incumbentSubProblems.get(j).length; i++)
				{
					//totalSubproblemsObjective+=this.subProblemsIncumbent.get(this.iterations)[i].ObjectiveValue;
					if (this.incumbentSubProblems.get(j)[i].nbAdmittedUE<this.incumbentSubProblems.get(j)[i].assignedUEs.length )
					{
						//this.cutChangePa(this.incumbentSubProblems.get(j)[i]);
						this.cutPreventAssigningLessPa(this.incumbentSubProblems.get(j)[i]);
					}					
				}
		}
		this.incumbentSubProblems =null;
		//reset incumbent array for the next iteration
		this.incumbentSubProblems = new ArrayList<Subproblem[]>();
	}
	
	/**
	 * Returns the total nb of used app at a certain iteration.
	 * app is used if it is assigned processing resources (verify that this correct and coherent with the value of n_a)
	 *
	 * @return nb of used app
	 */
	public int getTotalNbUsedApplication()
	{
		int totalUsedapp =0;
		
		//get all subproblems at the specified iteration
		for (int i=0; i<this.master.naValues.length; i++)
		{
			if(this.master.naValues[i]==1)
			{
				totalUsedapp++;
			}
			
		}
		return totalUsedapp;
	}
	
	/**
	 * Get the total processing resources assigned at a certain iteration
	 * 
	 * @return total processing resources in all mec
	 */
	public int getTotalAssignedPa()
	{
		int totalpa =0;
		
		for (int i=0; i<this.master.pa.length; i++)
		{
			totalpa+=this.master.pa[i];
			
		}
		return totalpa;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
