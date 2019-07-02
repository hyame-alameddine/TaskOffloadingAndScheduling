package generalClasses;

import helperClasses.Output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class DataGeneration {
	public int M; // nb of mobile edge cloud
	public int A; // nb of Apps
	public int T; // nb of types
	public int U; // nb of tasks/users
	public int processingCapacitiesNb;
	public int [] P; // set of processing capacities that can be assigned to f
	public int Delta; // timeline
	public int startTime; //the start time slot for next batch schedule
	
	public int[] cm;// capacity of MEC m
	public int[] ta; // type of app a
	public int[] pmina;// min processing capacity required for app a
	public int[] t_u; // user u asking function of type t
	public int[] theta_u;// deadline of user u
	public int[] mu_u; // nb of cycled required to process task of user u
	public int[] dup_u; // access/upload delay of user u
	public int[][] hum; // edge to edge transmission delay of user u to MEC m
	public int[][] xma;// mEC m hosting app a
	public int[][] sigma_ua; // access/upload+edge-edge delay of user u to app a
	public int[] thetamaxa; //max deadline of all tasks that can be assigned to a
	public int[] sigmamina;//min arrival of all tasks that can be assigned to a
	
	public Random rand;
	public int seed;
	
	public String inputsRanges;
	
	public DataGeneration (int seed, int M, int A, int T, int U, int processingCapacitiesNb, int Delta, int startTime, int mincm, int maxcm, int pmina_min,int pmina_max, int minDeadline, int maxDeadline, int minCycles, int maxCylces, int minAccessDelay, int maxAccessDelay, int minEdgeDelay, int maxEdgeDelay )
	{
		this.seed= seed;
		this.rand = new Random();
		this.rand.setSeed(this.seed);
		
		this.M = M;
		this.A = A;
		this.T = T;
		this.U = U;
		this.processingCapacitiesNb = processingCapacitiesNb;
		this.Delta = Delta;
		this.startTime = startTime;
		
		
		this.generateMECs(mincm, maxcm, pmina_min, pmina_max);
		this.generateUE(minDeadline, maxDeadline, minCycles, maxCylces, minAccessDelay, maxAccessDelay, minEdgeDelay, maxEdgeDelay);
		this.setSigmaUa();
		this.setSigmaMina();	
		this.setThetamaxa();
		
		this.inputsRanges="MECs Ranges:\n";
		this.inputsRanges+="\tcm\t["+mincm+"-"+maxcm+"]\n"
				+"\tpmina\t["+pmina_min+"-"+pmina_max+"]\n";
				
		this.inputsRanges+="UEs Ranges:\n";
		this.inputsRanges+="\ttheta_u\t["+minDeadline+"-"+maxDeadline+"]\n"
				+"\tmu_u\t["+minCycles+"-"+maxCylces+"]\n"
				+"\tdaccess\t["+minAccessDelay+"-"+maxAccessDelay+"]\n"
				+"\thum\t["+minEdgeDelay+"-"+maxEdgeDelay+"]\n";
	}
	
	
	public DataGeneration(int [] P, int Delta, int [] cm, int [] ta, int [] pmina,int [] pmaxa, int [] t_u, int[] theta_u, int[] mu_u, int[] dup_u, int[][] hum, int [][]xma, int startTime)  {
	
		this.M = cm.length;
		this.A = pmina.length;
		this.T = ta.length;
		this.U = mu_u.length;
		this.processingCapacitiesNb = P.length;
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
		this.sigma_ua = new int [this.U][this.A];
		this.sigmamina = new int [this.A];
		this.thetamaxa = new int [this.A];
		rand = new Random();
	}
	
	
	//when generating P make sure it has all the values of pmaxa since our cuts are based on pa =pmaxa 
	/**
	 * This  method will create and populate all the mec and app info.
	 * The set P should contain all values of pmina, pmaxa and randomly generated processing capacities between pmina_min and mincm
	 * prevent having 2 app of the same type on the same mec
	 * @param mincm should be at least equal to pmaxa_min
	 * @param maxcm should be at least equal to pmaxa_max
	 * @param pmina_min
	 * @param pmina_max
	 */
	public void generateMECs (int mincm, int maxcm, int pmina_min,int pmina_max )
	{
		int mec=0;
		int [] nbAppofType = new int [this.T];
		int [][] mecWithType = new int [this.M][this.T];
		int r ;
		int [] minProcessingPerMEC = new int [this.M];//sum of pmina per MEC to make sure that all applications on this MEC can get at least pmina
		Set <Integer>processingCapacities  = new HashSet<Integer>();//prevent duplicates of possible processing capacities since each one can be selected by mutiple apps
		
		this.xma = new int[this.M][this.A];
		this.ta = new int [this.A];
		this.pmina = new int [this.A];
		this.cm = new int [this.M];
		

		for (int m=0; m<this.M; m++)
		{
			this.cm[m] = this.rand.nextInt((maxcm-mincm)+1)+mincm; 
			
			//add cm to processing capacities
			processingCapacities.add(this.cm[m]);
		}
		
		//populate xma - loop through apps and decide on the mec hosting them
		for (int a=0; a<this.A; a++)
		{	
			 
			 //create an app of a random type from the list of available types, make sure to have at least one app of each type
			 //app ids are continious independant from mec id
			 //if the number of apps>types choose one app of each type and the rest randomly
			//Make sure not have more than the nb of MEC of a certain type as we want to prevent having 2 app of the same type on the same MEC
			if (a<this.T)
			{
				this.ta[a]=a ;
				nbAppofType[a]++;
			}
			else
			{
				//generate random type
				r= this.rand.nextInt(this.T);
				
				//choose another type if the nb of apps of this types reached the nb of mec to prevent having more than one app of a type on an mec
				while (nbAppofType[r] == this.M)
				{
					r=this.rand.nextInt(this.T);					
				}
				this.ta[a]= r;
				nbAppofType[r]++;
			}
			 
			this.pmina[a] = this.rand.nextInt((pmina_max-pmina_min)+1)+pmina_min;
		
				
			//choose a random mec to host the app a
			mec = rand.nextInt(this.M);
			
			//make sure that we don't have more than one type of each type on an mec and that all applications assigned can at least have the min processing capacity of the mec
			while (mecWithType[mec][this.ta[a]]==1 || this.cm[mec] < (minProcessingPerMEC[mec]+this.pmina[a]))
			{
				mec = rand.nextInt(this.M);
			}
			
					
			this.xma[mec][a]=1;	
			mecWithType[mec][this.ta[a]]++;
			minProcessingPerMEC[mec]+=this.pmina[a];
			
			
			//make sure that all pmina and cm are included in the set P
			processingCapacities.add(this.pmina[a]);
		}
		
		
		// generate few more random processing capacities based on the processingCapacitiesNb.
		//The generated ones should be between mincm (so an app can be hosted on any cm,(the max capacity it can acquire should not exceed mincm) and pmin_a
		for (int p=0; p<this.processingCapacitiesNb; p++)
		{
			processingCapacities.add( this.rand.nextInt((mincm-pmina_min)+1)+pmina_min);
		}
		
		/*Integer [] procCapacities = new Integer [processingCapacities.size()];
		this.P = new int [processingCapacities.size()];
		//convert to array 
		processingCapacities.toArray(procCapacities);
		
		//set the array P to the values in processingCapacities (includes all pmina, cm and random generated processing capacities)
		for (int p=0; p<this.P.length; p++)
		{
			this.P[p]= procCapacities[p];
		}*/
		int [] proc = {2,3,4,5,6,7,8,9,10,15,20};
		this.P = proc;
	}
	
	/**
	 * This function generates user equipments and considers:
	 * That at least one app of each type is hosted in the network
	 * The edge to edge delay are generated randomly (we may change that to consider shortest path)
	 * 
	 * @param minDeadline
	 * @param maxDeadline
	 * @param minCycles
	 * @param maxCylces
	 * @param minAccessDelay
	 * @param maxAccessDelay
	 * @param minEdgeDelay
	 * @param maxEdgeDelay
	 */
	public void generateUE (int minDeadline, int maxDeadline, int minCycles, int maxCylces, int minAccessDelay, int maxAccessDelay, int minEdgeDelay, int maxEdgeDelay)
	{
		int mec =0;
		this.t_u = new int[this.U];
		this.theta_u = new int[this.U];
		this.mu_u = new int[this.U];
		this.dup_u = new int[this.U];
		this.hum = new int[this.U][this.M];
		
		for (int u=0; u<this.U; u++)
		{
			//we consider that at least one VNF of each type is hosted, and that we choose a type between 0 and T (T exclusive)
			this.t_u [u] = this.rand.nextInt(this.T);
			
			//make sure that the deadline exceeds the start time of a batch
			this.theta_u[u] = this.rand.nextInt((maxDeadline-minDeadline)+1)+minDeadline+this.startTime;
			this.mu_u[u] = this.rand.nextInt((maxCylces-minCycles)+1)+minCycles;
			this.dup_u[u] = this.rand.nextInt((maxAccessDelay-minAccessDelay)+1)+minAccessDelay;
			
					
			for (int m=0; m< this.M; m++)
			{				
				this.hum [u][m] = this.rand.nextInt((maxEdgeDelay-minEdgeDelay)+1)+minEdgeDelay;
			}
			
			//set at least one edge to edge for each UE to 0 to specify 
			mec = this.rand.nextInt(this.M);
			this.hum [u][mec]=0;
		}
	}
	
	/**
	 * this method will set the arrival time of u to each app regardless if it is of the same type
	 */
	public void setSigmaUa()
	{
		this.sigma_ua = new int[this.U][this.A];
		int edge =0;
		for (int u=0; u<this.U; u++)
		{
			for (int a=0; a<this.A; a++)
			{
				for (int m=0; m<this.M; m++)
				{
					edge+=this.hum[u][m]*this.xma[m][a];
				}
				this.sigma_ua[u][a] = this.dup_u[u]+edge;
				edge=0;
			}
		}
	}
	
	/**
	 * this will set the min arrival time of all users that can be mapped to an app
	 */
	public void setSigmaMina()
	{
		this.sigmamina = new int[this.A];
		List<Integer> sigmaApp = new ArrayList<Integer>();
		for (int a=0; a<this.A; a++)
		{
			for (int u=0; u<this.U; u++)
			{	
				//get only those of the same type
				if (this.ta[a]!=this.t_u[u])
				{
					continue;
				}
				sigmaApp.add(this.sigma_ua[u][a]);				
			}
			
			Collections.sort(sigmaApp);
			this.sigmamina[a] = sigmaApp.size()!=0? sigmaApp.get(0):0;
			sigmaApp = null;
			sigmaApp = new ArrayList<Integer>();
		}
	}
	
	/**
	 * This method will set the max deadline of all tasks that can be mapped to an app
	 */
	public void setThetamaxa ()
	{
		this.thetamaxa = new int [this.A];
		List<Integer> deadlines = new ArrayList<Integer>();
		
		//loop over app
		for (int a= 0; a < this.A; a++) {
			
			//loop over users
			for (int u= 0; u < this.U; u++) {
				if (this.ta[a]!=this.t_u[u])
				{
					continue;
				}
			
				deadlines.add(this.theta_u[u]);
			}
			
			this.thetamaxa[a]=	deadlines.size()!=0?Collections.max(deadlines):0;
			deadlines = null;
			deadlines = new ArrayList<Integer>();
		}
	}
	
	
	public String toString()
	{
		String st ="DATA GENERATION INPUT RANGES\n";
		st+=this.inputsRanges;
		st += "\t Parameters:\n";
		st +="\t\tM:\t"+this.M+"\n";
		st +="\t\tA:\t"+this.A+"\n";
		st +="\t\tT:\t"+this.T+"\n";
		st +="\t\tU:\t"+this.U+"\n";
		st +="\t\tDELTA:\t"+this.Delta+"\n";
		st +="\t\tstartTime:\t"+this.startTime+"\n";
		st +="\t\tseed:\t"+this.seed+"\n";
		st +="\t\tprocessingCapacitiesNb:\t"+this.processingCapacitiesNb+"\n";
		st += Output.printTable(this.cm, "CAPACITY of MEC");
		st += Output.printDoubleMatrice(this.xma, "MEC m hosting apps");
		st +=  Output.printTable(this.ta, "app a OF TYPE t");
		st += Output.printTable(this.P, "App PROCESSING CAPACITIES ");		
		st +=  Output.printTable(this.pmina, "MIN CAPACITY of app a");
		st +=  Output.printTable(this.t_u, "UE u requesting app OF TYPE a");
		st += Output.printTable(this.mu_u, "Nb CYCLES OF USER U");
		st += Output.printTable(this.theta_u, "DEADLINE OF USER U");
		st += Output.printTable(this.dup_u, "UPLOAD DELAY OF USER U");
		st += Output.printDoubleMatrice(this.hum, "TRANSMISSION DELAYS OF USER u TO MEC m");
		st += Output.printDoubleMatrice(this.sigma_ua, "ARRIVAL OF USER u TO APP a");
		st += Output.printTable(this.sigmamina, "MIN ARRIVAL TO APP a");
		st += Output.printTable(this.thetamaxa, "MAX DEADLINE TO APP a");

		return st;
		
	}
	
	/**
	 * This method return the mec where the app is deployed
	 * returns -1 if mec not found (app not deployed)
	 * @param app
	 * @return
	 */
	public int getMECofApp (int app)
	{
		for (int m=0; m<this.xma.length; m++)
		{
			if(this.xma[m][app]==1)
			{
				return m;
			}
		}
		
		return -1;
	}
}
