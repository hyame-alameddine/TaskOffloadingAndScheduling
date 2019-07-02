/**
 * This model performs assignment of tasks to apps (each task is assigned one app) on mobile edge cloud in addition to task scheduling on apps 
 * and determining the processing capacity of each app 
 * The task asks for a single app (no chaining)
 * This model considers scheduling by batches by setting starttimeslot = delta
 * NOte: when debugging dprocess is assigned a upper value. If dprocess = 1.5 or 1.6, it will be assigned the value 2
 * 
 * If tasks arrival time = 2, process time = 3 and deadline =4 => the task will be rejected as 2+3=5>4, even though the processing will be done at 4
 * if it was accepted, the next task can start at 5
 * 
 * This model is tested and working
 */
package models;

import java.io.IOException;

import generalClasses.DataGeneration;
import helperClasses.FileManipulation;
import helperClasses.Output;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

public class LARAS_MIP {
	public IloCplex cplex; // IloCplex is the class used to create & solve the model
	public double objectiveValue;

	public int M; // nb of mobile edge cloud
	public int A; // nb of apps
	public int T; // nb of types
	public int U; // nb of tasks/users
	public int [] P; // set of processing capacities that can be assigned to a
	public int Delta; // timeline
	public int startTime; //the start time slot for next batch schedule
	
	public int[] cm;// capacity of MEC m
	public int[] ta; // type of function f
	public int[] pmina;// min processing capacity required for app a
	
	public int[] t_u; // user u asking app of type t
	public int[] theta_u;// deadline of user u
	public int[] mu_u; // nb of cycled required to process task of user u
	public int[] dup_u; // access/upload delay of user u
	public int[][] hum; // edge to edge transmission delay of user u to MEC m
	public int[][] xma;// App a is deployed on mEC m

	// decision variables
	public IloIntVar[][][] yuadelta;// task of user u started processing by app a at time slot delta
	public IloIntVar[] na;//app a is used
	 public IloIntVar [][] zap;//  app a is assigned the processing capacity p
	 public IloIntVar[][][] suupa;//u started processing b4 up on a
	// For linearization
	public IloIntVar[][][][] wpuad;// wpuad = sum_a sum_delta yuadelta zap
	public IloIntVar[][][] rhouupa;//rhouupa =sum_delta yuadelta sum_delta yupfdelta 

	public LARAS_MIP(int [] P, int Delta, int [] cm, int [] ta, int [] pmina, int [] t_u, int[] theta_u, int[] mu_u, int[] dup_u, int[][] hum, int [][]xma, int startTime) throws IloException {
		this.cplex = new IloCplex();
		this.objectiveValue = -1;

		this.M = cm.length;
		this.A = pmina.length;
		this.T = ta.length;
		this.U = mu_u.length;
		this.P = P;
		this.Delta = Delta;
		this.startTime = startTime;
		
		this.cm = cm;
		this.ta = ta;
		this.pmina = pmina;
		
		this.t_u = t_u;
		this.theta_u = theta_u;
		this.mu_u = mu_u;
		this.dup_u = dup_u;
		this.hum = hum;
		this.xma = xma;
		
		this.yuadelta = new IloIntVar[this.U][this.A][this.Delta];
		this.na= new IloIntVar[this.A];
		this.zap = new IloIntVar[this.A][this.P.length];
		this.suupa = new IloIntVar[this.U][this.U][this.A];
		
		this.wpuad = new IloIntVar[this.P.length][this.U][this.A][this.Delta];
		this.rhouupa = new IloIntVar [this.U][this.U][this.A];

		
	}
	
	
	/**
	 * This function initialize the decision variables
	 * 
	 * @throws IloException
	 */
	public void initializeDecisionVariables() throws IloException {
		
		for (int u= 0; u < this.U; u++) {
		
			for (int a= 0; a < this.A; a++) {
				for(int d= this.startTime; d < this.Delta; d++){
					this.yuadelta[u][a][d] = cplex.intVar(0, 1, "y_ufd[" + u + "][" + a + "]["+d+"]");
				}				
			}
		}

		for (int a = 0; a < this.A; a++) {
			this.na[a] = cplex.intVar(0, 1, "n_a[" + a + "]");
		
		}
		
		for (int a = 0; a < this.A; a++) {
			for (int p= 0; p < this.P.length; p++) {
				this.zap[a][p] = cplex.intVar(0, 1, "z_ap[" + a + "][" +p + "]");
			}
		}

		for (int u= 0; u < this.U; u++) {
			for (int up= 0; up < this.U; up++) {
				for (int a= 0; a < this.A;a++) {
				
					this.suupa[u][up][a] = cplex.intVar(0, 1, "s_uupf[" + u + "][" + up + "]["+a+"]");
				}				
			}
		}
		
		for (int u= 0; u < this.U; u++) {
			for (int p= 0; p < this.P.length; p++) {
				for (int a = 0;a < this.A; a++) {
					for(int d= this.startTime; d < this.Delta; d++){
						this.wpuad[p][u][a][d]= cplex.intVar(0, 1, "w_puad[" + p + "]["+u+"]["+a+"]["+d+"]" );
					}
				}
				
			}
		}
		
		for (int u= 0; u < this.U; u++) {
			for (int up= 0; up < this.U ;up++) {
				for (int a = 0;a < this.A; a++) {
					this.rhouupa[u][up][a] =  cplex.intVar(0, 1, "rho_uupa[" + u + "]["+up+"]["+a+"]" );
									
				}
			}
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
		 * Objective : max sum_u sum_f sum_d yufd[u][f][d]
		 */
		IloNumExpr objective = this.cplex.numExpr();
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
			//loop over vna
			for (int a= 0; a < this.A; a++) {
				// loop over time slots
				for(int d= this.startTime; d < this.Delta; d++){
					objective = this.cplex.sum(objective, this.yuadelta[u][a][d]); 
				}				
			}
		}

		// set objective function to maximize the number of admitted tasks
		this.cplex.addMaximize(objective);

		//loop over apps
		for (int a= 0; a < this.A; a++) {

			IloNumExpr appUsed= this.cplex.numExpr();
			for (int u= 0; u < this.U; u++) {		
				// loop over time slots
				for(int d= this.startTime; d < this.Delta; d++){
					appUsed = this.cplex.sum(appUsed, this.yuadelta[u][a][d]); 
				}	
				
			}
			//const 3: na<=sum_delta sum_u y_uad forall a  
			this.cplex.addLe(this.na[a], appUsed, "App "+a+ "is used") ;
			//const 4: Mna>=sum_delta sum_u y_uad forall a
			this.cplex.addGe(cplex.prod(Integer.MAX_VALUE, this.na[a]), appUsed, "App "+a+ "is used") ;
		}
		
		
		int appMaxCapacity =0;
		//loop over apps
		for (int a= 0; a < this.A; a++) {
			IloNumExpr appCapacity= this.cplex.numExpr();
			appMaxCapacity =0;		
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {
				appCapacity = this.cplex.sum(appCapacity, this.cplex.prod(this.zap[a][p], this.P[p]));
			}
			
			//Const 4: each app should be assigned its min processing capacity sum_p zap p >= na pmina forall a
			this.cplex.addGe(appCapacity, this.cplex.prod(this.na[a], this.pmina[a]), "App "+a+" is assigned at least its min processing capacity");
			
			for (int m=0; m<this.M; m++)
			{
				appMaxCapacity+=this.xma[m][a]*this.cm[m];
			}
				
			
			//Const 5: each app can be assigned at most the max processing capacity sum_p zap p <= na sum_m xma cm forall a
			this.cplex.addLe(appCapacity, this.cplex.prod(this.na[a], appMaxCapacity), "App "+a+" is assigned at most its max processing capacity");
		}
		
		//loop over mec
		for (int m = 0; m < this.M; m++) {
			IloNumExpr mecCapacityConst= this.cplex.numExpr();
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {								
				//loop over vna
				for (int a= 0; a < this.A; a++) {	
					//sum_p sum_a xma zap p
					mecCapacityConst = this.cplex.sum (mecCapacityConst, this.cplex.prod(this.xma[m][a], this.cplex.prod(this.zap[a][p], this.P[p])));
				}
			}
			
			//Const6: sum_p sum_a xma zap p<= cm for all m
			this.cplex.addLe(mecCapacityConst, this.cm[m],"MEC "+m+" capacity constraint" );
		}
		
	
		//loop over users
		for (int u= 0; u < this.U; u++) {
			IloNumExpr UEStartProcessonOneApp= this.cplex.numExpr();
			
			//loop over vna
			for (int a= 0; a < this.A; a++) {
				//loop over timeslots
				for(int d= this.startTime; d < this.Delta; d++){
					UEStartProcessonOneApp = this.cplex.sum(UEStartProcessonOneApp, this.yuadelta[u][a][d]);
				}				
			}			
			//Const 7: sum_asum_d yuad<=1 for all u
			this.cplex.addLe (UEStartProcessonOneApp, 1, " UE "+u+ "start processing on one app at one time slot");
		}
		
		IloNumExpr sumuadeltayuad= this.cplex.numExpr();
		//loop over users
		for (int u= 0; u < this.U; u++) {
			
			
			//loop over app
			for (int a= 0; a < this.A; a++) {
				
				//set yuad to 0 if type of app != type requested to user
				if (this.ta[a]==this.t_u[u])
				{
					continue;
				}
				//loop over timeslots
				for(int d= this.startTime; d < this.Delta; d++){
					sumuadeltayuad = this.cplex.sum (sumuadeltayuad, this.yuadelta[u][a][d]);
				}
			}
			
		}

		//Const 8: sum_u sum_a (ta!=t_u) sum_d yuad =0
		this.cplex.addLe(sumuadeltayuad, 0, " UEs can not be assigned to apps of different types than the requested one");
		
		
		//loop over app
		for (int a= 0; a < this.A; a++) {		
			//loop over timeslots
			for(int d= this.startTime; d < this.Delta; d++){
				IloNumExpr sumuyuad= this.cplex.numExpr();
				
				//loop over users
				for (int u= 0; u < this.U; u++) {
					sumuyuad = this.cplex.sum(sumuyuad, this.yuadelta[u][a][d]);
				}
				
				//Const 9: sum_u yuad <= 1 forall d forall a
				this.cplex.addLe(sumuyuad, 1, "App"+a+ "can at most process one UE at time slot "+d);
			
			}
		}
		
		IloNumExpr [] dprocess =  new IloNumExpr[this.U];
		//loop over users
		for (int u= 0; u < this.U; u++) {			
			dprocess[u] = this.cplex.numExpr();
			
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {
							
				//loop over vnf
				for (int a= 0; a < this.A; a++) {		
										
					//loop over timeslots
					for(int d= this.startTime; d < this.Delta; d++){
							
						//wpuad<=  yuad forall p forall u forall a forall d
						this.cplex.addLe(this.wpuad[p][u][a][d], this.yuadelta[u][a][d], "dprocces_u1 : yuad : wpuad for processing capacity = "+this.P[p]+" for UE "+u);
						
						// wpuad<= zap forall p forall u forall a forall d
						this.cplex.addLe(this.wpuad[p][u][a][d],  this.zap[a][p], "dprocces_u2 : zap : wpuad for processing capacity = "+this.P[p]+" for UE "+u);
						
						// wpuad>= yuad +  zap-1  forall p forall u forall a forall d
						this.cplex.addGe(this.wpuad[p][u][a][d], this.cplex.sum(this.yuadelta[u][a][d], this.cplex.diff( this.zap[a][p], 1))," dprocces_u3: sumazap+sumazap-1 : wpuad for processing capacity = "+this.P[p]+" for UE "+u);	
					
						// dprocess = sum_p sum_a sum_d wpuad mu/p forall u
						dprocess[u] = this.cplex.sum(dprocess[u],this.cplex.prod (this.wpuad[p][u][a][d], Math.ceil((double)this.mu_u[u]/this.P[p])));
					}
				}
			}
		}
		
		//loop over users
		for (int u= 0; u < this.U; u++) {	
			//loop over up up!=u
			for (int up= 0; up < this.U; up++) {
				if (u ==up)
				{
					continue;
				}
				
				// add only if u and u' are of the same type 
				if(this.t_u[u]!=this.t_u[up])
				{
					continue;
				}
				//loop over apps
				for (int a= 0; a < this.A; a++) {
					
					// add only if u, u' and a are  of the same type 
					if(this.t_u[u]!=this.ta[a])
					{
						continue;
					}
					IloNumExpr sumdeltayuad= this.cplex.numExpr();
					IloNumExpr sumdeltapyupad= this.cplex.numExpr();
					IloNumExpr sumdyuad= this.cplex.numExpr();
					IloNumExpr sumdpyupad= this.cplex.numExpr();
					
					//loop over timeslots
					for(int d= this.startTime; d < this.Delta; d++){
						sumdeltayuad = this.cplex.sum(sumdeltayuad, this.cplex.prod(this.yuadelta[u][a][d], d));
						sumdeltapyupad = this.cplex.sum(sumdeltapyupad, this.cplex.prod(this.yuadelta[up][a][d], d));
						
						sumdyuad = this.cplex.sum(sumdyuad, this.yuadelta[u][a][d]);
						sumdpyupad = this.cplex.sum(sumdpyupad, this.yuadelta[up][a][d]);
						
					}
					
					// sum delta yuad>=sumdelta yupad+dp_up-H(1-supua) forall a \in A, u, up \in U qua=qupa
					this.cplex.addGe(sumdeltayuad,this.cplex.sum(sumdeltapyupad, this.cplex.diff(dprocess[up], this.cplex.prod(Integer.MAX_VALUE,this.cplex.diff(1, this.suupa[up][u][a])))), "sumdeltayuad>=sumdeltapyupad+dp_up-H(1-supua)");
					
					// sumdelta yupad >=sumdelta yuad +dp_up-H(1-suupa) forall a \in A, u, up \in U qua=qupa
					this.cplex.addGe(sumdeltapyupad,this.cplex.sum(sumdeltayuad, this.cplex.diff(dprocess[u], this.cplex.prod(Integer.MAX_VALUE,this.cplex.diff(1, this.suupa[u][up][a])))), "sumdeltayupad >=sumdeltapyuad +dp_up-H(1-suupa)");
					
					// suupa+supua =\sum_delta yuad \sum delta yupad forall a \in A, u, up \in U qua=qupa
					
					this.cplex.addLe (this.rhouupa[u][up][a],sumdyuad, "this.rhouupa[u][up][a]<= sum_dyuad" );
					this.cplex.addLe (this.rhouupa[u][up][a],sumdpyupad , "this.rhouupa[u][up][a]<= sum_dyupad");
					this.cplex.addGe (this.rhouupa[u][up][a],this.cplex.sum(sumdyuad,this.cplex.diff(sumdpyupad,1)), "rhouupa >= sum_d yuad+sum_dyupad -1");
					
					this.cplex.addEq(this.cplex.sum(this.suupa[u][up][a], this.suupa[up][u][a]),this.rhouupa[u][up][a], "supua+suupa=rhouupa[u][up][a]");
				}
							
			}
			
					
			IloNumExpr sumadeltayuad= this.cplex.numExpr();
			//loop over app
			for (int a= 0; a < this.A; a++) {						
				//loop over timeslots
				for(int d= this.startTime; d < this.Delta; d++){
					sumadeltayuad = this.cplex.sum(sumadeltayuad, this.cplex.prod(this.yuadelta[u][a][d], d));
				}
				
				//  sum_a sum_d yufd d+ dprocess^u <= theta_u forall u
				this.cplex.addLe(this.cplex.sum(sumadeltayuad, dprocess[u]), this.theta_u[u],"task of UE "+u+" processed before deadline");
				
			}				
			
		}
		
		
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
			IloNumExpr dedge = this.cplex.numExpr();
			IloNumExpr sumadeltayuad= this.cplex.numExpr();
			IloNumExpr sumadeltayuadByd= this.cplex.numExpr();
			
			//loop over app
			for (int a= 0; a < this.A; a++) {	
				//loop over timeslots
				for(int d= this.startTime; d < this.Delta; d++){
					sumadeltayuad = this.cplex.sum(sumadeltayuad, this.yuadelta[u][a][d]);
					sumadeltayuadByd = this.cplex.sum(sumadeltayuadByd, cplex.prod(this.yuadelta[u][a][d], d));
					
				}				
			}
			
			//loop over mec
			for (int m = 0; m < this.M; m++) {		
				//loop over app
				for (int a= 0; a < this.A; a++) {
					
					//loop over timeslots
					for(int d= this.startTime; d < this.Delta; d++){	
						dedge = this.cplex.sum(dedge, cplex.prod(this.yuadelta[u][a][d],this.xma[m][a]*this.hum[u][m]));						
					}
				}
				
				//Const14: sum_a sum_d yuad (dupu+startTime)+d_edge<= sum_a sum_d yuad
				this.cplex.addLe (this.cplex.sum(this.cplex.prod(sumadeltayuad,this.dup_u[u]+this.startTime), dedge), sumadeltayuadByd,"Const14: Task of UE "+u +" can not start processing before it is transmitted" );
			}
			
			
		}
		
		
		//loop over app
		for (int a= 0; a < this.A; a++) {
			IloNumExpr sumpzap= this.cplex.numExpr();
			
			//loop over possible processing capacities
			for (int p= 0; p < this.P.length; p++) {
				sumpzap = this.cplex.sum(sumpzap, this.zap[a][p]);
			}
			
			//Const 18: sum_p zap =1 forall a
			this.cplex.addLe(sumpzap, 1, "choose one value for processing capacity for Vna "+a);
		}
		
	}
	
	/**
	 * This method will run the ILP model Export the model to a file called
	 * SchedulinRoutingDeadline.lp Report the results (values of the variables) to a
	 * file called SchedulinRoutingDeadlineResult.lp
	 * 
	 * @param resultsFile
	 *            path to the file where to dump results
	 * @throws IloException
	 */
	public double [] runILPModel(String resultsFile) throws IloException {
		double [] results= new double [5];
		try {
			
			this.cplex.exportModel("LARAILP_model.lp");

			if (cplex.solve()) {
				this.objectiveValue = this.cplex.getObjValue();

				System.out.println("solved " + this.objectiveValue);
				System.out.println(this.cplex.getStatus());

				// print results (values of the decision variables)
				this.reportResults(resultsFile);
			
				//report results
				results[0] = this.objectiveValue;		
				results[1] =0;//exec time will be assigned later in results run
				results[2] = this.getTotalNbUsedApplication();
				results[3] = this.getTotalAssignedPa();
				
				
			} else {
				this.objectiveValue = -1;
				 System.out.println(this.cplex.getStatus());
				System.out.println(" NOT solved ");
			}
			// this.cplex.end();

		} catch (IloException e) {
			System.out.println("ERROR RUNNING ILP Model");
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * This method will return an array with the processing capacities assigned to each app by the sub problem
	 * 
	 * @return apps processing capacities
	 */
	public int[] getAppsProcessingCapacities()
	{
		int [] appCapacities = new int [this.A];
		int capacity =0;
		int tempzap =0;
		try {
			//loop over functions
			for (int a= 0; a < this.A; a++) {
				
				//loop over capacities
				for (int p=0; p<this.P.length; p++)
				{
					//start by performing some rounding (just in case)
					tempzap=this.cplex.getValue(zap[a][p])>=0.9?1:0;					
					capacity+=tempzap*this.P[p];
					tempzap=0;
				}				
				appCapacities[a] = capacity;
				capacity=0;
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return appCapacities;
	}
	
	/**
	 * Get the total processing resources assigned
	 * 
	 * @return total processing resources in all mec
	 */
	public int getTotalAssignedPa()
	{
		int [] appCapacities = this.getAppsProcessingCapacities();
		int totalpa =0;
		
		for (int i=0; i<appCapacities.length; i++)
		{
			totalpa+=appCapacities[i];
			
		}
		return totalpa;
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
		
		
			try {
				//get all subproblems at the specified 
				for (int a=0; a<this.A; a++)
				{
					if(this.cplex.getValue(this.na[a])>=0.9)
					{
						totalUsedapp++;
					}
				}
			} catch (UnknownObjectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
		return totalUsedapp;
	}
	
	public String toString() {
		String st = "";
		st += " ILP MODEL \n";
		st += "\t Parameters:\n";

		st += Output.printTable(this.cm, "CAPACITY of MEC");
		st += Output.printTable(this.P, "App PROCESSING CAPACITIES ");
		st += Output.printDoubleMatrice(this.xma, "App a placed on MEC m");
		st +=  Output.printTable(this.ta, "App a OF TYPE t");		
		st +=  Output.printTable(this.pmina, "MIN CAPACITY of App a");

		st +=  Output.printTable(this.t_u, "UE u requesting App OF TYPE t");
		st += Output.printTable(this.mu_u, "NUMBER OF CYCLES OF USER U");
		st += Output.printTable(this.theta_u, "DEADLINE OF USER U");
		st += Output.printTable(this.dup_u, "Upload delay for task of user u ");
		st += Output.printDoubleMatrice(this.hum, "TRANSMISSION DELAYS OF USER u TO MEC m");
		
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
	public void reportResults(String filePath) {
		String st = "";

		st += this.toString();

		// print results
		st += "\t RESULTS\n";
		try {
			st += String.format("\t\t Objective = %f\n", this.cplex.getObjValue());
		
			st += "\t\t" + Output.printTripleArray(this.yuadelta, "y[U][A][D]", "\t\t y", this.cplex);
			st += "\t\t" + Output.printArray(this.na, "na[a]", "\t\t n", this.cplex);
			st += "\t\t" + Output.printDoubleArray(this.zap, "zap[A][P]", "\t\t z", this.cplex);
			st += "\t\t" + Output.printTripleArray(this.suupa, "suupa[U][Up][A]", "\t\t s", this.cplex);
			st += "\t\t" + Output.printQuadrupleArray(this.wpuad, "wpu[P][U][A][D]", "\t\t w", this.cplex);
			st += "\t\t" + Output.printTripleArray(this.rhouupa, "rhouupa[U][U][A]", "\t\t rho", this.cplex);		
			// write everything in a file
			FileManipulation outputFile = new FileManipulation(filePath);
			outputFile.writeInFile(st);
			System.out.println(st);

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) throws IloException {
		/*int [] P = { 2,4,6,8}; //P can not contain 0
		int Delta = 20;
		int [] cm = {0,10, 20, 15, 0};
		int [] tf = {1,5, 3, 4};//1
		int [] pmina = {2, 3, 4, 2};
		int [] t_u = {1, 1,5,5};//5
		int [] theta_u = {20, 15,8,7};
		int [] mu_u = {20,10,15,30};
		int [] dup_u = {2,2,1,1};
		int [][]hum = {
				{0, 2, 4, 6, 8},
				{8, 6, 4, 1, 0},	
				{8, 4, 3, 4, 2},
				{4, 5, 0, 1, 2},
		};
		
		int [] P = { 2,4,6,8, 10,30}; //P can not contain 0
		int Delta = 50;
		int [] cm = {0,6,0, 20, 30};
		int [] tf = {1,2,1,4,6};//1
		int [] pmina = {2,4,3,1,1};
		int [] pmaxa = {40,20,10,4,8};
		int [] t_u = {1,1,5,4,4,3};//5
		int [] theta_u = {11,18, 20, 30,19,44};
		int [] mu_u = {20,16,40,30,5, 15};
		int [] dup_u = {2,1,4,5,3,5};
		int [][]hum = {
				{3,2,0,3,9 },
				{2,1,0,4,2},
				{0,3,5,6,9 },
				{2,5,3,0,3},
				{5,2,8,3,0 },
				{7,0,8,3,10 },	
		};
		
		int [] P = { 20}; //P can not contain 0
		int Delta = 50;
		
		int start=1;
		int [] cm = {40, 20};
		int [] tf = {1,2,3,4,6};//1
		int [] pmina = {40,4,3,1,1};
		int [] pmaxa = {40,20,10,4,8};
		int [] t_u = {1,2};//5
		int [] theta_u = {10,4};
		int [] mu_u = {40,20};
		int [] dup_u = {1,1};
		int [][]hum = {
				{3,0},
				{10,2}
		};
		int [][] xfm={{1,0},{0,1},{1,0},{1,0},{0,1}};*/
		
		
	/*	int [] P = {10, 20}; //P can not contain 0
		int Delta = 10;
		
		int start=2;
		int [] cm = {20, 20};
		int [] tf = {1,2,3,4,6};//1
		int [] pmina = {10,4,3,1,1};
		int [] pmaxa = {40,20,10,4,8};
		int [] t_u = {1,1};//5
		int [] theta_u = {10,15};
		int [] mu_u = {40,20};
		int [] dup_u = {1,1};
		int [][]hum = {
				{3,1},
				{10,1}
		};
		int [][] xfm={{0,1},{0,1},{1,0},{1,0},{0,1}};*/
		//int[][]quf={{0,1,0,0,0},{0,1,0,0,0}};
		int [] P = {5,10,15,25}; 
		int Delta = 15;
		
		int start=0;
		int [] cm = {20,15};
		int [] ta = {1,2,3,5,2,6};//1
		int [] pmina = {10,5,5,5,5,10};//{5,5,10,10,5,5};
		int [] pmaxa = {15,10,15,15,15,20};//{7,10,15,20,15,15};
		int [] t_u = {2,2,2,3,3};//5
		int [] theta_u = {5,3,8,8,9};
		int [] mu_u = {20,10,15,30, 5};
		int [] dup_u = {1,2,4,2,1};
		int [][]hum = {
				{2,0},
				{0,4},
				{3,0},
				{0,4},
				{3,0}
		};
		int [][] xma={{1,1,0,1,0,0},{0,0,1,0,1,1},};
		
		/*int seed = 12;
		int M=2;
		int A=3;
		int T=2;
		int U=4;
		int processingCapacitiesNb=5;
		int Delta=20;
		int startTime=0;
		int mincm=10;
		int maxcm=20;
		int pmina_min=2;
		int pmina_max=5;
		int pmaxa_min=5;
		int pmaxa_max=10;
		int minDeadline=5;
		int maxDeadline=15;
		int minCycles =10;
		int maxCylces=50;
		int minAccessDelay=1;
		int maxAccessDelay=2;
		int minEdgeDelay=1;
		int maxEdgeDelay=3;
		DataGeneration data= new DataGeneration( seed,  M,  A,  T,  U,  processingCapacitiesNb,  Delta,  startTime,  mincm,  maxcm,  pmina_min, pmina_max,  pmaxa_min, pmaxa_max, minDeadline,  maxDeadline,  minCycles,  maxCylces,  minAccessDelay,  maxAccessDelay,  minEdgeDelay,  maxEdgeDelay);
		LARAS_MIP ilp = new LARAS_MIP(data.P, data.Delta, data.cm, data.ta, data.pmina,data.pmaxa, data.t_u, data.theta_u, data.mu_u, data.dup_u, data.hum, data.xma, data.startTime);
		*/LARAS_MIP ilp = new LARAS_MIP(P, Delta, cm, ta, pmina, t_u, theta_u, mu_u, dup_u, hum, xma, start);
		ilp.buildILPModel();
		ilp.runILPModel("ilpRes.txt");
		
	}
	
	
	
}
