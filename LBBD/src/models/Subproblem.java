/**
 * This model will only perform the scheduling of UE assigned to one application.
 * yu may take a value even if the task is rejected
 */
package models;

import java.io.IOException;

import helperClasses.FileManipulation;
import helperClasses.Output;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import bendersDecomposition.User;

public class Subproblem extends Thread{
	
	public IloCplex cplex; // IloCplex is the class used to create & solve the model
	public double objectiveValue;
	
	public int U;//nb of admitted tasks
	public int a; //application id representing the subproblem
	public int pa; // processing capacity assigned by master to a
	public User[] assignedUEs;//assigned ue by master
	public int iteration; //iteration in benders
	
	public int nbAdmittedUE;
	public double execTime;
	
	public IloIntVar [] alpha_u;
	public IloIntVar [] yu;
	public IloIntVar [][] suupa;
	public IloIntVar [][] rhouup;
	
	public Subproblem(int a, int pa, User[] assignedUEs, int iteration) throws IloException
	 {
		this.cplex = new IloCplex();
		this.objectiveValue = 0;
		
		 this.a=a;
		 this.pa =pa;
		 this.assignedUEs = assignedUEs;
		 this.iteration = iteration;	
		 this.nbAdmittedUE =0;
		 this.execTime =0;
		 this.U = this.assignedUEs.length;
		 
		 this.alpha_u = new IloIntVar[this.U];
		 this.yu = new IloIntVar[this.U];
		 this.suupa = new IloIntVar[this.U][this.U];
		 this.rhouup = new IloIntVar[this.U][this.U];
	 }
	
	
	/**
	 * This function initialize the decision variables
	 * 
	 * @throws IloException
	 */
	public void initializeDecisionVariables() throws IloException {
		
		for (int u=0; u<this.U; u++)
		{
			this.alpha_u[u] = this.cplex.intVar(0, 1, "alpha_u[" + u + "]");
			this.yu[u] = this.cplex.intVar(0, Integer.MAX_VALUE, "y_u[" + u + "]");
			
			for (int up=0; up<this.U; up++)
			{
				this.suupa[u][up] = this.cplex.intVar(0, 1, "suup[" + u + "]["+up+"]");
				this.rhouup[u][up] = this.cplex.intVar(0, 1, "suup[" + u + "]["+up+"]");
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
		 * Objective : max sum_u sum_a qua[u][a]
		 */
		IloNumExpr objective = this.cplex.numExpr();
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
						
			objective = this.cplex.sum(objective, this.alpha_u[u]); 
		}
		
		// set objective function to maximize the number of admitted tasks
		//max sum_u alpha_u
		this.cplex.addMaximize(objective);
		
		//loop over users
		for (int u= 0; u < this.U; u++) {
			
			//yu>= sigma_u^A alpha_u forall u 
			this.cplex.addGe(this.yu[u], this.cplex.prod(this.alpha_u[u], this.assignedUEs[u].sigma), "User "+this.assignedUEs[u].idInMaster+" can start ");
			
			//yu +dproces [u] alpha_u<= theta_u forall u 
			this.cplex.addLe(this.cplex.sum(this.yu[u], this.cplex.prod(this.alpha_u[u], (int)Math.ceil((double)this.assignedUEs[u].mu/this.pa))),this.assignedUEs[u].theta , "User "+this.assignedUEs[u].idInMaster+" meet the deadline ");
		
			
			for (int up= 0; up < this.U; up++) {
				
				if (u==up)
				{
					continue;
				}
				
				// yu>= yup+dproce_up alpha[up] - H(1-suup) for all u, up u<> up
				this.cplex.addGe (this.yu[u], this.cplex.sum(this.yu[up], this.cplex.diff(
						this.cplex.prod(this.alpha_u[up], (int)Math.ceil((double)this.assignedUEs[up].mu/this.pa)), 
						this.cplex.prod(Integer.MAX_VALUE, this.cplex.diff (1,this.suupa[up][u]))					
						)),
						"Precedence constraint1 on UE upb4u" +this.assignedUEs[up].idInMaster+ "and UE u"+this.assignedUEs[u].idInMaster
				);
				
				//yup>= yu+dproce_u alpha_u-H(1-suup) forall u, up u<>up
				this.cplex.addGe (this.yu[up], this.cplex.sum(this.yu[u], this.cplex.diff(
						this.cplex.prod(this.alpha_u[u], (int)Math.ceil((double)this.assignedUEs[u].mu/this.pa)), 
						this.cplex.prod(Integer.MAX_VALUE, this.cplex.diff (1,this.suupa[u][up]))					
						)),
						"Precedence constraint2 on UE ub4up" +this.assignedUEs[u].idInMaster+ "and UE u"+this.assignedUEs[up].idInMaster
				);
				
				//rho_uup<= alpha_up forall u, up u<>up
				this.cplex.addLe( this.rhouup[u][up], this.alpha_u[up], "rho_uup LE alpha_up");
				
				//rho_uup<=rho_u forall u, up u<>up
				this.cplex.addLe( this.rhouup[u][up], this.alpha_u[u], "rho_uup LE alpha_u");
				
				//alpha_uup>= alpha_u+alpha_up-1 forall u, up u<>up
				this.cplex.addGe( this.rhouup[u][up], this.cplex.diff(this.cplex.sum(this.alpha_u[u], this.alpha_u[up]), 1), "rho_uup GE alpha_u+alpha_up-1");
				
				//suup+supu = rhouup forall u, up u<>up
				this.cplex.addEq(this.cplex.sum(this.suupa[u][up], this.suupa[up][u]), this.rhouup[u][up], "One UE b4 the other");
			}
		}
		
	}	
	
	@Override
	public String toString()
	{
		String st ="Subproblem for app "+this.a+"\n";
		st+="\tpa ="+this.pa+"\n";
		st+="\tU ="+this.U+"\n";
		st+="\tIteration ="+this.iteration+"\n";
		st+="\tNbAdmitedUE ="+this.nbAdmittedUE+"\n";
		for (int u=0; u<this.assignedUEs.length; u++)
		{
			st+="\t"+this.assignedUEs[u].toString()+"\n";
		}
		st+="\n";
		return st;
		
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
	public void run() {
		try {
			//this.cplex.setOut(null);
			this.cplex.exportModel("subProblem_model.lp");

			if (cplex.solve()) {
			//	FileManipulation resultsFile = new FileManipulation ("testResults/Benders_"+this.iteration+".txt");
				FileManipulation resultsFile = new FileManipulation ("testResults/Benders-detailed.txt");
				this.objectiveValue = this.cplex.getObjValue();

				System.out.println("solved " + this.objectiveValue);
				System.out.println(this.cplex.getStatus());
					
				//set results
				this.setAdmittedUser();
				
				// print results (values of the decision variables)
				this.reportResults(resultsFile);
			} else {
				this.objectiveValue = 0;
				 System.out.println(this.cplex.getStatus());
				System.out.println(" NOT solved ");
			}
			// this.cplex.end();

		} catch (IloException e) {
			System.out.println("ERROR RUNNING ILP Model");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * This method will report the ILP inputs and results to a file called
	 * ILP/ILPResults_setId It will also print the results to the console
	 * 
	 * @param outputFile
	 *           the file where to write the results
	 *            ("ILP/ILPResults_"+setId+".txt")
	 */
	public void reportResults(FileManipulation outputFile) {
		String st = "";

		st += this.toString();
		st += "Exection Time \t"+this.execTime+"\n";
		// print results
		st += "\t RESULTS\n";
		try {
			st += String.format("\t\t Objective = %f\n", this.cplex.getObjValue());
		
		
			st += "\t\t" + Output.printArray(this.alpha_u, "alpha_u[U]", "\t\t alpha", this.cplex);
			st += "\t\t" + Output.printArray(this.yu, "yu[U]", "\t\t yu", this.cplex);
			st += "\t\t" + Output.printDoubleArray(this.suupa, "suupa[U]", "\t\t suup", this.cplex);
			st += "\t\t" + Output.printDoubleArray(this.rhouup, "rhouup[U]", "\t\t rhouup", this.cplex);
			
					
			// write everything in a file
			//FileManipulation outputFile = new FileManipulation(filePath);
			outputFile.writeInFile(st);
			System.out.println(st);

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
	
	
	/**
	 * this method will check if a user is assigned to the subproblem app by the master
	 * @param userId
	 * @return boolean
	 */
	public boolean isAssigned (int userId) 
	{
				
		for (int u=0; u<this.assignedUEs.length; u++)
		{
			if (this.assignedUEs[u].idInMaster == userId)
			{
				return true;
			}
		}
			return false;
	
	}
	
	/**
	 * This method will set admitted and start schedule for admitted user by a
	 * 
	 * @throws IloException 
	 * @throws UnknownObjectException 
	 */
	public User [] setAdmittedUser () 
	{
		User [] admittedUE = new User [this.nbAdmittedUE];
		try {
			for (int u=0; u<this.assignedUEs.length; u++)
			{
				
				if (this.cplex.getValue(this.alpha_u[u])>0.9)
				{
					this.nbAdmittedUE++;
					this.assignedUEs[u].admitted = true;
					this.assignedUEs[u].startSchedule = (int) this.cplex.getValue(this.yu[u]);
				}
				
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return admittedUE;
	}
	
	/**
	 * This method will return an array of admitted user by a
	 * @return array of admitted users
	 */
	public User [] getAdmittedUser ()
	{
		User [] admittedUE = new User [this.nbAdmittedUE];
		
		for (int u=0; u<this.assignedUEs.length; u++)
		{
			if (this.assignedUEs[u].admitted)
			{
				admittedUE[u]=this.assignedUEs[u];
			}
		}
		
		return admittedUE;
	}

}
