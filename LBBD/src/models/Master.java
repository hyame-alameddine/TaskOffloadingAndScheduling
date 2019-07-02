package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import generalClasses.DataGeneration;
import helperClasses.FileManipulation;
import helperClasses.Output;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

public class Master {
	public IloCplex cplex; // IloCplex is the class used to create & solve the model
	public double masterObjectiveValue;

	public int M; // nb of mobile edge cloud
	public int A; // nb of apps
	public int T; // nb of types
	public int U; // nb of tasks/users
	public int J; //maximum number of possible orientation btw all apps, tasks (maximum length of dprocess_ua in dprocess_uaj), Usually the maximum is deadline-arrival for each user
	
	public int [] P; // set of processing capacities that can be assigned to a
	public int [] cm;//mec capacity
	public int[] ta; // type of app a
	public int[] pmina;// min processing capacity required for app a
	public int[] t_u; // user u asking function of type t
	public int[][] xma;// app a is deployed on mEC m
	public int[] mu_u; // nb of cycled required to process task of user u
	public int[] theta_u;// deadline of user u
	public int[][] sigma_ua; // access/upload+edge-edge delay of user u to app a
	public int[] thetamaxa; //max deadline of all tasks that can be assigned to a
	public int[] sigmamina;//min arrival of all tasks that can be assigned to a
	public ArrayList<ArrayList<ArrayList<Integer>>> P_uaj;//set of processing capacities that allows u to meet its deadline on a 
	public ArrayList<ArrayList<ArrayList<Integer>>> dprocess_uaj;//set of processing times that allows u to meet its deadline on a if P_uaj is selected-not really needed
	
	public IloIntVar[][] qua;// task of user u mapped to app a 
	public IloIntVar[] na;// app a is used
	public IloIntVar [][] zap;//  app a is assigned the processing capacity p
	public IloIntVar [][][]beta_uaj;//if user u on app a is using orientation j (P_uaj, dprocess_uaj)
	
	public int beta_uajNb;//number of beta_uajVariables-needed for branching
	public IloNumVar [] beta_uajVariables;//holds the created beta_uaj variables- needed for branching
	
	//results
	public int [][][]beta_uajValues;
	public int [][]quaValues;
	public int [][]zapValues;
	public int[] naValues;
	public int[] pa;
	int it;
	
	public Master (int [] ta, int [] pmina, int [] P, int []cm , int [] t_u, int [][]xma, int[] mu_u, int [] theta_u, int [][]sigma_ua, int[] sigmamina, int [] tethamaxa) throws IloException
	{
		this.cplex = new IloCplex();
		this.masterObjectiveValue = 0;

		this.M = xma.length;
		this.A = pmina.length;
		this.T = ta.length;
		this.U = t_u.length;
		this.ta = ta;
		this.pmina = pmina;
		this.P=P;
		this.cm = cm;
		this.t_u = t_u;
		this.xma = xma;
		this.mu_u = mu_u;		
		this.theta_u = theta_u;
		this.sigma_ua = sigma_ua;
		this.thetamaxa = tethamaxa;
		this.sigmamina = sigmamina;
		this.P_uaj = new  ArrayList<ArrayList<ArrayList<Integer>>>();
		this.dprocess_uaj = new  ArrayList<ArrayList<ArrayList<Integer>>>();
		
		//set the values Puaj and deprocess_uaj
		this.setPuajDprocessuaj();
		this.setJ();
		
		this.beta_uaj = new IloIntVar [this.U][this.A][this.J];
		this.qua = new IloIntVar[this.U][this.A];
		this.na= new IloIntVar[this.A];
		this.zap = new IloIntVar[this.A][this.P.length];
		
		
		
		this.quaValues = new int [this.U][this.A];
		this.zapValues = new int [this.A][this.P.length];
		this.naValues = new int [this.A];
		this.pa = new int[this.A];
		this.beta_uajValues=new int [this.U][this.A][this.J];
	}
	
	public Master (DataGeneration data) throws IloException
	{
		this.cplex = new IloCplex();
		this.masterObjectiveValue = 0;
		
		this.M = data.M;
		this.A = data.A;
		this.T = data.T;
		this.U = data.U;
		this.ta = data.ta;
		this.pmina = data.pmina;
		this.P=data.P;
		this.cm = data.cm;
		this.t_u = data.t_u;
		this.xma = data.xma;
		this.mu_u = data.mu_u;		
		this.theta_u = data.theta_u;
		this.sigma_ua = data.sigma_ua;
		this.thetamaxa = data.thetamaxa;
		this.sigmamina = data.sigmamina;
		this.P_uaj = new  ArrayList<ArrayList<ArrayList<Integer>>>();
		this.dprocess_uaj = new  ArrayList<ArrayList<ArrayList<Integer>>>();
		
		//set the values Puaj and deprocess_uaj - this will sort P
		this.setPuajDprocessuaj();
		this.setJ();
		
		this.beta_uaj = new IloIntVar [this.U][this.A][this.J];
		this.qua = new IloIntVar[this.U][this.A];
		this.na= new IloIntVar[this.A];
		this.zap = new IloIntVar[this.A][this.P.length];
		

		this.quaValues = new int [this.U][this.A];
		this.zapValues = new int [this.A][this.P.length];
		this.naValues = new int [this.A];
		this.pa = new int[this.A];
		this.beta_uajValues=new int [this.U][this.A][this.J];
	}
	
	/**
	 * This function initialize the decision variables
	 * 
	 * @throws IloException
	 */
	public void initializeDecisionVariables() throws IloException {
		
		//declare first so cplex will start branching on them
		for (int u= 0; u < this.U; u++) {		
			for (int a= 0; a < this.A; a++) {
				
				//this user can not be scheduled on this app-> do not create a variable
				if (this.P_uaj.get(u).get(a)==null)
				{
					continue;
				}
				
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.P_uaj.get(u).get(a).size(); j++)
				{
					this.beta_uaj[u][a][j] = cplex.intVar(0, 1, "beta_uaj[" + u + "][" + a + "]["+j+"]");
					this.beta_uajNb++;
				}
				
			}
		}
		
		for (int u= 0; u < this.U; u++) {		
			for (int a= 0; a < this.A; a++) {
				this.qua[u][a] = cplex.intVar(0, 1, "q_ua[" + u + "][" + a + "]");
			}
		}
		
		for (int a = 0; a< this.A; a++) {
			this.na[a] = cplex.intVar(0, 1, "n_a[" + a + "]");
		
		}
		
		for (int a = 0; a < this.A; a++) {
			for (int p= 0; p < this.P.length; p++) {
				this.zap[a][p] = cplex.intVar(0, 1, "z_ap[" + a + "][" +p + "]");
			}
		}
			

	}
	
	/**
	 * this method will tell cplex to branch on beta_uaj.
	 * NOT WORKING
	 */
	public void SOS1Branching()
	{
		int count=0;
		this.beta_uajVariables = new IloIntVar[this.beta_uajNb];
		int [] priorities = new int [this.beta_uajNb];
		
		for (int u= 0; u < this.U; u++) {		
			for (int a= 0; a < this.A; a++) {
				
				//this user can not be scheduled on this app-> do not create a variable
				if (this.P_uaj.get(u).get(a)==null)
				{
					continue;
				}
				
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.P_uaj.get(u).get(a).size(); j++)
				{
					this.beta_uajVariables[count] = this.beta_uaj[u][a][j] ;
					priorities[count] = count+1;//set increasing priorities because we can not set the same priority, starting 1
					count++;
				}
				
			}
		}
		
		try {
			//this.cplex.addSOS1(this.beta_uajVariables , priorities,"SOS1 variables");
			this.cplex.setPriorities(this.beta_uajVariables,priorities);
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This function will build the model by building the constraints
	 * 
	 * @throws IloException
	 */
	public void buildILPModel() throws IloException {
		this.initializeDecisionVariables();

		/**
		 * Objective : max sum_u sum_a qua[u][a]
		 */
		IloNumExpr objective = this.cplex.numExpr();
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
			//loop over apps
			for (int a= 0; a< this.A; a++) {				
				objective = this.cplex.sum(objective, this.qua[u][a]); 
			}				
		}

		// set objective function to maximize the number of admitted tasks
		this.cplex.addMaximize(objective);
		
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
			IloNumExpr UESProcessonOneApp= this.cplex.numExpr();
			
			//loop over app
			for (int a= 0; a < this.A; a++) {			
				UESProcessonOneApp = this.cplex.sum(UESProcessonOneApp, this.qua[u][a]);
						
			}			
			//sum_a qua<=1 for all u
			this.cplex.addLe (UESProcessonOneApp, 1, " UE "+u+ "can process on at most one app");
		}
		
		
		IloNumExpr sumaqua= this.cplex.numExpr();
		//loop over users
		for (int u= 0; u < this.U; u++) {			
			//loop over app
			for (int a= 0; a < this.A; a++) {
				
				//set qua to 0 if type of app != type requested to user
				if (this.ta[a]==this.t_u[u])
				{
					continue;
				}	
				sumaqua = this.cplex.sum (sumaqua, this.qua[u][a]);				
			}		
			
		}
		//sum_u sum_a (ta!=t_u) qua =0 
		this.cplex.addEq(sumaqua, 0, " UEs can not be assigned to apps of different types than the requested one");
		
		//loop over app
		for (int a= 0; a < this.A; a++) {

			IloNumExpr functionUsed= this.cplex.numExpr();
			for (int u= 0; u < this.U; u++) {		
				functionUsed = this.cplex.sum(functionUsed, this.qua[u][a]); 							
			}
			
			// na<=sum_u qua
			this.cplex.addLe(this.na[a], functionUsed, "App "+a+ "is used") ;
			// Mna>=sum_u qua forall a 
			this.cplex.addGe(cplex.prod(Integer.MAX_VALUE, this.na[a]), functionUsed, "App "+a+ "is used2") ;
		}
		
		int appMaxCapacity =0;
		//loop over apps
		for (int a= 0; a < this.A; a++) {
			IloNumExpr functionCapacity= this.cplex.numExpr();
			appMaxCapacity =0;
			
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {
				functionCapacity = this.cplex.sum(functionCapacity, this.cplex.prod(this.zap[a][p], this.P[p]));
			}
			
			// each app should be assigned its min processing capacity sum_p zap p >= na pmina forall a
			this.cplex.addGe(functionCapacity, this.cplex.prod(this.na[a], this.pmina[a]), "App "+a+" is assigned at least its min processing capacity");
			
			for (int m=0; m<this.M; m++)
			{
				appMaxCapacity+=this.xma[m][a]*this.cm[m];
			}
						
			//each app can be assigned at most the max processing capacity sum_p zap p <= na sum_m xma cm forall a
			this.cplex.addLe(functionCapacity, this.cplex.prod(this.na[a], appMaxCapacity), "App "+a+" is assigned at most its max processing capacity");
		}
		
		
		for (int m=0; m<this.M; m++)
		{
			IloNumExpr mecCapacityConst= this.cplex.numExpr();
			
			//loop over apps
			for (int a= 0; a < this.A; a++) {	
				//loop over possible processing capacities
				for (int p= 0; p < this.P.length; p++) {	
					//sum_p sum_a xma zap p
					mecCapacityConst = this.cplex.sum (mecCapacityConst,  this.cplex.prod(this.zap[a][p], this.P[p]*this.xma[m][a]));
				
				}
			}
			
			//sum_p sum_a xma zap p<= cm 
			this.cplex.addLe(mecCapacityConst, this.cm[m],"MEC "+m+" capacity constraint" );
		}
	
		//loop over users
	/*	for (int u= 0; u < this.U; u++) {		
			
			//loop over apps
			for (int a= 0; a < this.A; a++) {	
				
				if (this.t_u[u]!=this.ta[a])
				{
					continue;
				}
				IloNumExpr processCapacity= this.cplex.numExpr();
				
				//loop over possible processing capacities
				for (int p= 0; p < this.P.length; p++) {	
					processCapacity = this.cplex.sum(processCapacity, this.cplex.prod (this.zap[a][p], this.P[p]));
				}
				//sum_p zap P[p] *(theta_u-sigma_ua)>= mu quafor all u, a
				this.cplex.addGe(this.cplex.prod(processCapacity,(this.theta_u[u]-this.sigma_ua[u][a])), this.cplex.prod(this.mu_u[u], this.qua[u][a]), "Processing capacity of app "+a+" for user "+u);
				
			}
		}*/
		
		//loop over apps
		for (int a= 0; a < this.A; a++) {	
			IloNumExpr processCapacity= this.cplex.numExpr();
			IloNumExpr cycles= this.cplex.numExpr();
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {	
				processCapacity = this.cplex.sum(processCapacity, this.cplex.prod (this.zap[a][p], this.P[p]));
			}
			
			//loop over users
			for (int u= 0; u < this.U; u++)	{
				if (this.t_u[u]!=this.ta[a])
				{
					continue;
				}
				cycles = this.cplex.sum(cycles, this.cplex.prod(this.mu_u[u], this.qua[u][a]));
			}
			
			//this means no tasks can be assigned to this app (no one asking for this type), prevent division by 0
			if (this.thetamaxa[a]==0)
			{
				continue;
			}
			//sum_p zap p *(thetamaxa -signmamina)
			this.cplex.addGe(this.cplex.prod(processCapacity,(this.thetamaxa[a]-this.sigmamina[a])), cycles, "Processing capacity of app "+a+" greater than all processing of tasks ");
		}
		
		
		//loop over apps
		for (int a= 0; a < this.A; a++) {
			IloNumExpr sumpzap= this.cplex.numExpr();
			
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {
				sumpzap = this.cplex.sum(sumpzap, this.zap[a][p]);
			}
			
			// sum_p zap <=1 forall a
			this.cplex.addLe(sumpzap, 1, "choose one value for processing capacity for app "+a);
		}
		
		
		//prevent user to be assigned to tasks of the same type which can not meet its deadline (min processing capacity required can not be provided to app)
		IloNumExpr sumuaqua= this.cplex.numExpr();
		for (int u= 0; u < this.U; u++) {
			
			for (int a= 0; a < this.A; a++) {
				
				//check only app of same type
				if (this.t_u[u] != this.ta[a])
				{
					continue;
				}
				
				if (this.P_uaj.get(u).get(a)==null)
				{
					sumuaqua = this.cplex.sum(sumuaqua, this.qua[u][a]);
				}
				
			}
			
		}
		//sum_{u \in Bar{U}, a:tu=ta}qua =0
		this.cplex.addEq(sumuaqua, 0, "Prevent user to be assigned to app of same type if they can not meet their deadline");
		
		/**
		 * Additional constraints that will help with the cut
		 */
		for (int u= 0; u < this.U; u++) {		
			for (int a= 0; a < this.A; a++) {
				IloNumExpr sumjbetauaj= this.cplex.numExpr();
				IloNumExpr sumjbetauajpuaj= this.cplex.numExpr();
				IloNumExpr sumpzapp= this.cplex.numExpr();
				
				for (int p=0; p<this.P.length; p++)
				{
					sumpzapp = this.cplex.sum(sumpzapp, this.cplex.prod(this.zap[a][p], this.P[p]));
				}
				
				//this user can not be scheduled on this app-> do not create a variable
				if (this.dprocess_uaj.get(u).get(a)==null)
				{
					continue;
				}
				
				//create variables only for existing values in dprocess_uaj
				for (int j=0; j<this.dprocess_uaj.get(u).get(a).size(); j++)
				{
					sumjbetauaj = this.cplex.sum(sumjbetauaj, this.beta_uaj[u][a][j]);
					sumjbetauajpuaj = this.cplex.sum(sumjbetauajpuaj, this.cplex.prod( this.beta_uaj[u][a][j], this.P_uaj.get(u).get(a).get(j)));
				}
				
				//sum_j beta_uaj = q_ua for all u, a
				this.cplex.addEq(sumjbetauaj, this.qua[u][a], "One processing is used for user "+u+" on app "+a);
				
				//sum_p zap*p>= sum_j p_uaj*beta_uaj  forall a, u
				this.cplex.addGe(sumpzapp,sumjbetauajpuaj,"pa greater than pa_uj of user "+u+"on app "+a );
			}
		}
		
		
		//Tell cplex to branch on beta_uaj - NOT WORKING
		//this.SOS1Branching();
		
	}
	
	
	/**
	 * This method will run the ILP model Export the model to a file called
	 * SchedulinRoutingDeadline.lp Report the results (values of the variables) to a
	 * file called SchedulinRoutingDeadlineResult.lp
	 * 
	 * @param resultsFile
	 *            the file where to dump results
	 * @throws IloException
	 */
	public void runILPModel(FileManipulation resultsFile) throws IloException {
		try {
//			this.cplex.setOut(null);
			//this.cplex.exportModel("master_model+"+it+".lp");
			this.cplex.exportModel("master_model.lp");
		//	it++;
			//show node by node in the displayed logs  and see on which variables it is branching
			//this.cplex.setParam(IloCplex.IntParam.MIPInterval, 1);
			//this.cplex.setParam(IloCplex.IntParam.Threads, 1);;
			if (cplex.solve()) {
				this.masterObjectiveValue = this.cplex.getObjValue();
				
				System.out.println("MASTER solved " + this.masterObjectiveValue);
				System.out.println(this.cplex.getStatus());
				
				this.setResults();				
				
			} else {
				
				 System.out.println(this.cplex.getStatus());
				System.out.println("MASTER  NOT solved ");
			}
			System.out.println("REPORTING MASTER RES ");
			// print results (values of the decision variables)
			this.reportResults(resultsFile);
			
			// this.cplex.end();

		} catch (IloException e) {
			System.out.println("ERROR RUNNING Master Model");
			e.printStackTrace();
		}

	}
	
	
	public String toString() {
		String st = "";
		st += " Master MODEL \n";
		st += "\t Parameters:\n";
		
		st += "\tMaximum Possible Processing capacities options J ="+this.J+"\n";
		st += Output.printTable(this.P, "POSSIBLE PROCESSING CAPACITIES ");
		st += Output.printTable(this.cm, "MEC Capacity ");
		st += Output.printDoubleMatrice(this.xma, " MEC m hosting app a");
		st += Output.printTable(this.pmina, "App Min PROCESSING CAPACITIES ");
		st +=  Output.printTable(this.ta, "App a OF TYPE t");
		
		st +=  Output.printTable(this.t_u, "UE u requesting App OF TYPE t");
		st +=  Output.printTable(this.mu_u, "Nb of cycles of UE u");
		st += Output.printTable(this.theta_u, "Deadline of task of user u ");
		
		st += Output.printDoubleMatrice(this.sigma_ua, "ARRIVAL OF USER u TO APP a");
		st += Output.printTable(this.sigmamina, "MIN ARRIVAL of USERS to APP a ");	
		st += Output.printTable(this.thetamaxa, "MAX Deadline of task of users on app a ");
		
		st +=Output.printTripleArrayList (this.dprocess_uaj, "Processing Time per user u","dprocess_uaj" );
		st +=Output.printTripleArrayList (this.P_uaj, "Processing capacities per user u", "P_uaj");
		
		return st;

	}
	
	
	/**
	 * This method will report the ILP inputs and results to a file called
	 * ILP/ILPResults_setId It will also print the results to the console
	 * 
	 * @param filePath
	 *            path to the file where to write the results
	 *            ("ILP/ILPResults_"+setId+".txt")
	 */
	public void reportResults( FileManipulation outputfile) {
		String st = "";

		st += this.toString();

		// print results
		st += "\t RESULTS\n";
		
		st += String.format("\t\t Objective = %f\n", this.masterObjectiveValue);
		
		st += "\t\t" + Output.printDoubleArray(this.quaValues, "qua[U][A]", "\t\t q");		
		st += "\t\t" + Output.printDoubleArray(this.zapValues, "zap[A][P]", "\t\t z");
		st += "\t\t" + Output.printArray(this.pa, "pa[A]", "\t\t p");
		st += "\t\t" + Output.printArray(this.naValues, "na[A]", "\t\t n");
		st += String.format("\t\t Number of beta_uaj variables = %d\n", this.beta_uajNb);
		st += "\t\t" + Output.printTripleArray(this.beta_uajValues, "beta_uaj[U][A][J]", "\t\t beta_uaj");
		
		// write everything in a file
	//	FileManipulation outputFile = new FileManipulation(filePath);
		outputfile.writeInFile(st);
		//System.out.println(st);	

	}
	
	/**
	 * set the values based on decision variables
	 */
	public void setResults ()
	{
		int capacity =0;
		
		try {
			//loop over functions
			for (int a= 0; a < this.A; a++) {
				
				//loop over capacities
				for (int p=0; p<this.P.length; p++)
				{
					//start by performing some rounding (just in case)
					this.zapValues[a][p]=this.cplex.getValue(zap[a][p])>=0.9?1:0;										
					capacity+=this.zapValues[a][p]*this.P[p];					
				}				
				this.pa[a] = capacity;
				capacity=0;
				
				this.naValues[a] = this.cplex.getValue(this.na[a])>=0.9?1:0;
				
				for (int u=0;u<this.U; u++)
				{
					this.quaValues[u][a]=this.cplex.getValue(this.qua[u][a])>=0.9?1:0;
				}
			}
			
			for (int u= 0; u < this.U; u++) {		
				for (int a= 0; a < this.A; a++) {
					
					//this user can not be scheduled on this app-> do not create a variable
					if (this.P_uaj.get(u).get(a)==null)
					{
						continue;
					}
					
					//create variables only for existing values in dprocess_uaj
					for (int j=0; j<this.P_uaj.get(u).get(a).size(); j++)
					{
						this.beta_uajValues[u][a][j] = this.cplex.getValue(this.beta_uaj[u][a][j])>=0.9?1:0;						
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
	

	
	/**
	 * This method will sort P
	 * It will set the values for  P_uaj and dprocess_uaj
	 */
	public void setPuajDprocessuaj ()
	{
		//set of processing capacities that can be used to schedule u from the set P
		ArrayList<ArrayList<Integer>> Pu = new ArrayList<ArrayList<Integer>>();
		//their associate processing time for u to meet its deadline
		ArrayList<ArrayList<Integer>> dprocess_u = new ArrayList<ArrayList<Integer>>();
		
		//processing per app
		ArrayList<Integer>pua = new ArrayList<Integer>();
		//processing time per app corresponding to pua
		ArrayList<Integer>dprocess_ua = new ArrayList<Integer>();
		
		int pmin =0;//min processing capacity required on a to meet deadline of u
		int dpmax =0;//max processing time to meet deadline of u on a
		int indexStartProcessing =0; // index in P of pmin
		int []pmax = new int[this.A];//max ppa for each app based on cm on which it is hosted
		
		//Sort P so we are able loop over it starting index of pmin
		Arrays.sort(this.P);
		
		for (int a=0; a<this.A; a++)
		{
			for (int m=0; m<this.M; m++)
			{
				pmax[a]+=this.xma[m][a]*this.cm[m];
			}
		}
		
		for (int u=0; u<this.U; u++)
		{		
			for (int a=0; a<this.A; a++)
			{
				if (this.ta[a]!=this.t_u[u])
				{ 
					//specify that u can not be assigned to a
					Pu.add(null);
					dprocess_u.add(null);
					
					continue;
				}
				
				//the minimum processing capacity needed by u to meet its deadline if it uses the whole time (deadline-arrival)
				dpmax = this.theta_u[u]-this.sigma_ua[u][a];
				pmin = (int) Math.ceil((double)this.mu_u[u]/dpmax);
				
				
				//get the first value in P >= pmin and add it to pua
				indexStartProcessing= this.getpminInP(pmin);
				
				//no processing capacity exists in P >= pmin, so u can not be scheduled on a
				if (indexStartProcessing == -1)
				{
					//specify that u can not be assigned to a
					Pu.add(null);
					dprocess_u.add(null);
					
					continue;
				}
				
				//if the min processing capacity is > then the max that can be assigned to the app on which the task can be deployed, the task can not use this pa and can not be deployed on a
				if (this.P[indexStartProcessing]>pmax[a])
				{
					Pu.add(null);
					dprocess_u.add(null);
					continue;
				}
				
				
				//loop over available processing capacities >= pmin
				for (int p=indexStartProcessing; p<this.P.length; p++)
				{
				
					//if the processing capacity is > then the max that can be assigned to the app on which the task can be deployed, the task can not use this pa
					if (this.P[p]>pmax[a])
					{
						break;
					}
					
					pmin = this.P[p];
					pua.add(pmin);
					
					//calculate the new processing time corresponding the new pmin and add it to dprocess_ua
					dpmax = (int) Math.ceil((double)this.mu_u[u]/pmin);
					dprocess_ua.add(dpmax);
				}
				
				//add arrays 
				Pu.add(pua);
				dprocess_u.add (dprocess_ua);
				
				//reinitialize
				pua = new ArrayList<Integer>();
				dprocess_ua = new ArrayList<Integer>();
			}
			
			
			//Add it to the array of all users
			this.P_uaj.add(Pu);
			this.dprocess_uaj.add(dprocess_u);
			
			//reset
			Pu = new ArrayList<ArrayList<Integer>>();
			dprocess_u = new ArrayList<ArrayList<Integer>>();
			
		}
	}
	
	
	/**
	 * maximum number of possible orientation btw all apps, tasks (maximum length of dprocess_ua in dprocess_uaj),
	 *  Usually the maximum is deadline-arrival for each user
	 *
	 */
	public void setJ()
	{
		this.J =0;
		for (int u=0; u<this.U; u++)
		{
			for  (int a=0; a<this.A; a++)
			{
				if (this.dprocess_uaj.get(u).get(a)==null)
				{
					continue;
				}
				this.J =this.dprocess_uaj.get(u).get(a).size()>this.J?(this.dprocess_uaj.get(u)).get(a).size():this.J;
			}
		}
	}
	
	/**
	 * This method will return the index in P of the 1st lowest value >= p
	 * P should be sorted in ascending order
	 * 
	 * @param p processing capacity to which we want P[i]>= p
	 * @return index i, -1 if no P[i]>= p
	 */
	public int getpminInP (int p)
	{
		//sorted P
		for(int i=0; i<this.P.length; i++)
		{
			if (this.P[i]>= p)
			{
				return i;
			}
		}
		
		return -1;
	}
	
	
	/**
	 * This method will return an array of UE assigned to each app
	 * @return
	 */
	public ArrayList<ArrayList<Integer>> getAdmittedUEsPertApp()
	{
		 ArrayList<ArrayList<Integer>> admittedUEsPertApp = new ArrayList<ArrayList<Integer>>();
		 ArrayList<Integer> appUE = new ArrayList<Integer>();
		
			 for (int a=0; a<this.A; a++)
			 {
				 for (int u=0; u<this.U; u++)
				 {
					 if (this.quaValues[u][a]>0.9)
					 {
						 appUE.add(u);
					 }					
				 }
				 
				 admittedUEsPertApp.add(appUE);
				 appUE = new ArrayList<Integer>();
			 }
		 		 
		 return admittedUEsPertApp;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 /*int[] ta = {0,1} ; // type of app a
		 int[] pmaxa = {8,10};// max processing capacity that can be allocated for app a
		 int[][] xma ={{1,1}};// app a is deployed on mEC m
		
		 int[] t_u={1,1}; // user u asking function of type t		 
		 int[] mu_u = {59,54}; // nb of cycled required to process task of user u
		 int[] theta_u ={9,9};// deadline of user u
		 int[] dup_u = {2,1}; // access/upload delay of user u
		 int[][] hum={{1},{1}}; // edge to edge transmission delay of user u to MEC m
		 int[] cm ={10};*/
		try {
			 int [] seed ={100};//{100};
			    int M=2;
			    int A=4 ;
			    int T=4;
			    int  U=5;//{5,10,15,20};
			    int processingCapacitiesNb=10;
			    int Delta=15;
			    int startTime=0;
			    int mincm=50;
			    int maxcm=50;
			    int pmina_min=10;
			    int pmina_max=20;
			   
			    int minDeadline=6;
			    int maxDeadline=15;
			    int minCycles =30;
			    int maxCylces=70;
			    int minAccessDelay=1;
			    int maxAccessDelay=2;
			    int minEdgeDelay=1;
			    int maxEdgeDelay=3;
			
			DataGeneration data= new DataGeneration( seed[0],  M,  A,  T,  U,  processingCapacitiesNb,  Delta,  startTime,  mincm,  maxcm,  pmina_min, pmina_max,  minDeadline,  maxDeadline,  minCycles,  maxCylces,  minAccessDelay,  maxAccessDelay,  minEdgeDelay,  maxEdgeDelay);
		/*int[]p={137,9 };
		int[]pmin = {12,4,5,5,5};
		data.P=p;
		data.pmina = pmin;*/
			System.out.println(data);
			Master master = new Master(data);
			master.buildILPModel();
			master.runILPModel(new FileManipulation("master.txt"));
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
