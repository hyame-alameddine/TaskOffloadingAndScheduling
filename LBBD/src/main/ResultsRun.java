package main;

import java.io.IOException;

import models.LARAS_MIP;
import generalClasses.DataGeneration;
import helperClasses.FileManipulation;
import bendersDecomposition.LogicBasedBenders;

public class ResultsRun {

	
	
	
	/**
	 * IMPORTANT: we allow one app of each type on an mec. Make sure the number of app chosen is aligned with this
	 * Make sure that cm_min is big enough so that all app can be assigned their min processing capacity
	 * @param algorithm benders, LARAS_MIP, bendersMasterIncumbent
	 * @throws Exception
	 */
	public static void run ( String algorithm) throws Exception
	{
		String parametersFile = "results/parameters.txt";
		String resultsFile = "results/"+algorithm+".txt";
		double []results = new double [5];
		double start = 0;
		double endTime = 0;
		
		DataGeneration data;
		LogicBasedBenders method;
		
		LARAS_MIP ilp ;
		
		//Main and decomp not same
		/*int [] seed = {24};//12
		int M=3;
		int A=12;
		int T=3;
		int U=20;
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
		
		//1053 iterations with adding the types
		int [] seed = {33};
		int M=2;
		int A=5;
		int T=2;
		int U=20;
		int processingCapacitiesNb=5;
		int Delta=20;//15
		int startTime=0;
		int mincm=5;
		int maxcm=15;
		int pmina_min=2;
		int pmina_max=5;
		int pmaxa_min=5;
		int pmaxa_max=10;
		int minDeadline=5;
		int maxDeadline=15;
		int minCycles =10;
		int maxCylces=30;
		int minAccessDelay=1;
		int maxAccessDelay=2;
		int minEdgeDelay=1;
		int maxEdgeDelay=3;*/
		
		/*int [] seed = {30};
		int M=3;
		int A=5;
		int T=1;
		int U=15;
		int processingCapacitiesNb=5;
		int Delta=30;
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
		int maxEdgeDelay=3;*/
		
		/*int [] seed ={100};//{//12//prob 13-e-e:2-5
		int M=3;
		int A=15 ;//15
		int T=5;
		int [] U={50};//{50, 60, 70, 80};//{5,10,15,20,25};//,50
		int processingCapacitiesNb=10;//5;
		int Delta=25;//50;//20;
		int startTime=0;
		int mincm=100;
		int maxcm=100;//20;/50
		int pmina_min=2;
		int pmina_max=5;
		int pmaxa_min=5;
		int pmaxa_max=50;//10;
		int minDeadline=15;//5;
		int maxDeadline=25;//15;
		int minCycles =10;
		int maxCylces=50;
		int minAccessDelay=1;
		int maxAccessDelay=2;
		int minEdgeDelay=1;//2;
		int maxEdgeDelay=3;//5;*/
		
		//Example not optimal solution
		/*int [] seed ={100};//{//12//prob 13-e-e:2-5
		int M=3;
		int A=15 ;
		int T=5;
		int [] U={12};//{5,10,15,20};//{5,10,15};//{20,30,40,50};//{50, 60, 70, 80};//{5,10,15,20,25};//,50
		int processingCapacitiesNb=10;//5;
		int Delta=15;//50;//20;
		int startTime=0;
		int mincm=20;
		int maxcm=20;
		int pmina_min=2;
		int pmina_max=5;
		int minDeadline=5;
		int maxDeadline=15;
		int minCycles =30;
		int maxCylces=70;
		int minAccessDelay=1;
		int maxAccessDelay=2;
		int minEdgeDelay=1;//2;
		int maxEdgeDelay=3;//5;*/
		
		//Example for the presentation to samir
		/*int [] seed ={100};
	    int M=3;
	    int A=6 ;
	    int T=2;
	    int [] U={10};
	    int processingCapacitiesNb=10;
	    int Delta=15;
	    int startTime=0;
	    int mincm=10;
	    int maxcm=10;
	    int pmina_min=2;
	    int pmina_max=5;
	    int pmaxa_min=5;
	    int pmaxa_max=10;
	    int minDeadline=5;
	    int maxDeadline=15;
	    int minCycles =50;
	    int maxCylces=70;
	    int minAccessDelay=1;
	    int maxAccessDelay=2;
	    int minEdgeDelay=1;
	    int maxEdgeDelay=3;//*/
		 
		/* int [] seed ={35};
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={25};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=20;
		  int maxcm=20;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =20;
		  int maxCylces=100;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;*/
		/*int [] seed ={100};
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={10};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=30;
		  int maxcm=30;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =30;
		  int maxCylces=70;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
		
		/* int [] seed ={12};
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={15};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=30;
		  int maxcm=30;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =30;
		  int maxCylces=70;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
	
	/*	int [] seed ={33};//{100};12
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={15};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=20;
		  int maxcm=20;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =30;
		  int maxCylces=70;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
		
		/*int [] seed ={44};//{100};12
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={20};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=30;
		  int maxcm=30;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =30;
		  int maxCylces=70;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
		
		/*int [] seed ={44};//{100};12
		  int M=8;
		  int A=20 ;
		  int T=5;
		  int [] U={50};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=30;
		  int maxcm=80;
		  int pmina_min=10;
		  int pmina_max=20;
		  int minDeadline=6;
		  int maxDeadline=15;
		  int minCycles =20;
		  int maxCylces=100;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
	 
		 /*int [] seed ={35};
		  int M=3;
		  int A=15 ;
		  int T=5;
		  int [] U={25};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=15;
		  int startTime=0;
		  int mincm=20;
		  int maxcm=20;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=5;
		  int maxDeadline=15;
		  int minCycles =20;
		  int maxCylces=100;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;//5;*/
	/*	int [] seed ={44,55,100,22,60};//{100};12
		  int M=8;
		  int [] A={10,15,20};
		  int T=5;
		  int [] U={70};//{5,10,15,20};
		  int processingCapacitiesNb=10;
		  int Delta=20;
		  int startTime=0;
		  int mincm=20;
		  int maxcm=20;
		  int pmina_min=2;
		  int pmina_max=5;
		  int minDeadline=10;
		  int maxDeadline=50;
		  int minCycles =20;
		  int maxCylces=100;
		  int minAccessDelay=1;
		  int maxAccessDelay=2;
		  int minEdgeDelay=1;//2;
		  int maxEdgeDelay=3;*/
		  
			/*int [] seed ={23};//{5,23,100,36,60};//{100};12, 133,22
			  int M=6;
			  int []A={20};//{10,15,20};//,25
			  int T=5;
			  int  U=50;//{5,10,15,20,25};//{5,10,15,20};
			  int processingCapacitiesNb=10;
			  int Delta=20;
			  int startTime=0;
			  int mincm=20;
			  int maxcm=20;
			  int pmina_min=2;
			  int pmina_max=5;
			  int minDeadline=20;
			  int maxDeadline=50;
			  int minCycles =50;
			  int maxCylces=200;
			  int minAccessDelay=1;
			  int maxAccessDelay=2;
			  int minEdgeDelay=1;//2;
			  int maxEdgeDelay=3;*/
		
		 int [] seed ={551,9,8,5,40};//{551,9,8,5,40};//6//{13,35,100,22,60};
	     int M=10;//{4,5,6};//3
	     int A=15;
	     int T=5;
	     int [] U={ 50};//30,40, 50
	     int processingCapacitiesNb=10;
	     int Delta=20;
	     int startTime=0;
	     int mincm=20;
	     int maxcm=20;
	     int pmina_min=2;
	     int pmina_max=5;
	     int minDeadline=15;
	     int maxDeadline=20;
	     int minCycles =20;
	     int maxCylces=100;
	     int minAccessDelay=1;
	     int maxAccessDelay=2;
	     int minEdgeDelay=1;//2;
	     int maxEdgeDelay=3;

		ResultsRun.reportResults(resultsFile, parametersFile, results, null, 0, minEdgeDelay, maxEdgeDelay, true);
		
		for (int i=0; i<U.length; i++)
		{
			for(int j=0; j<seed.length; j++)
			{
				 data= new DataGeneration( seed[j],  M,  A,  T,  U[i],  processingCapacitiesNb,  Delta,  startTime,  mincm,  maxcm,  pmina_min, pmina_max,  minDeadline,  maxDeadline,  minCycles,  maxCylces,  minAccessDelay,  maxAccessDelay,  minEdgeDelay,  maxEdgeDelay);
			
				 if(algorithm.equals("benders"))
				{
				 	method = new LogicBasedBenders(data);
					results = method.solve(false);
					System.out.println("Benders no incumbent runtime "+results[1]);
					System.out.println("Benders iterations "+results[4]);
				}
				 if(algorithm.equals("bendersMasterIncumbent"))
				{
				 	method = new LogicBasedBenders(data);
					results = method.solve(true);
					System.out.println("Benders incumbent runtime "+results[1]);
					System.out.println("Benders iterations "+results[4]);
				}
				else if (algorithm.equals("LARAS_MIP"))
				{
					start = System.currentTimeMillis();
					
					 ilp = new LARAS_MIP(data.P, data.Delta, data.cm, data.ta, data.pmina, data.t_u, data.theta_u, data.mu_u, data.dup_u, data.hum, data.xma, data.startTime);
					ilp.buildILPModel();
					results= ilp.runILPModel("ilpRes.txt");
					
					endTime= System.currentTimeMillis();
					results[1]= (int)(endTime-start);
					start=0;
					endTime=0;
					System.out.println("ILP runtime "+results[1]);
					
				}
				
				ResultsRun.reportResults(resultsFile, parametersFile, results, data, seed[j], minEdgeDelay, maxEdgeDelay, false);
			}
		}
	}
	
	
	/**
	 * This method returns the total cm of all mec
	 * @param data
	 * @return
	 */
	public  static int getMECcapacities (DataGeneration data)
	{
		int total =0;
		
		for (int i=0; i<data.cm.length; i++)
		{
			total+=data.cm[i];
		}
		
		return total;
	}
	
	/**
	 * 
	 * @param resultsFileName
	 * @param results
	 * @param data
	 * @param setId
	 * @param minEdgeDelay
	 * @param maxEdgeDelay
	 * @param headerOnly
	 */
	public static void reportResults(String resultsFileName, String parametersFileName, double [] results, DataGeneration data ,int setId,int minEdgeDelay, int maxEdgeDelay, boolean headerOnly)
	{	
		FileManipulation resultsFile;
		FileManipulation parametersFile;
		try {
			resultsFile = new FileManipulation(resultsFileName);
			parametersFile = new FileManipulation(parametersFileName);
			
			
			if (headerOnly)
			{
				
				 resultsFile.writeInFile("Set Id\t");
				 resultsFile.writeInFile("MEC Nb\t");
				 resultsFile.writeInFile("App Types Nb\t");
				 resultsFile.writeInFile("App Nb\t");
				 resultsFile.writeInFile("UEs Nb\t");
				 resultsFile.writeInFile("Edge-Edge Margin\t");
				 resultsFile.writeInFile("Nb of Used Apps\t");
				 resultsFile.writeInFile("Total assigned processing capacity\t");
				 resultsFile.writeInFile("Total existing MEC capacities\t");
				 resultsFile.writeInFile("Total Utilization of resources (%)\t");
				 resultsFile.writeInFile("ExecutionTime (ms)\t");
				 resultsFile.writeInFile("Admitted UEs NB \t");	
				 resultsFile.writeInFile("Admission Rate (%)\t");	
				 resultsFile.writeInFile("Nb of iterations\t");	
				 resultsFile.writeInFile("\n");
			}
			else
			{

				int totalMecCapacities = ResultsRun.getMECcapacities(data);
				double resourcesUtilization = ((double)(results[3])/totalMecCapacities)*100;
				double admissionRate = ((double)(results[0])/data.U)*100;
				
				parametersFile.writeInFile("SET "+setId+"---------------------\n");
				parametersFile.writeInFile(data.toString());
				
				 resultsFile.writeInFile("\n");
				 resultsFile.writeInFile(setId+"\t");
				 resultsFile.writeInFile(data.M+"\t");
				 resultsFile.writeInFile(data.T+"\t");
				 resultsFile.writeInFile(data.A+"\t");
				 resultsFile.writeInFile(data.U+"\t");
				 resultsFile.writeInFile("["+minEdgeDelay+"-"+maxEdgeDelay+"]\t");
				 resultsFile.writeInFile(results[2]+"\t");
				 resultsFile.writeInFile(results[3]+"\t");
				 resultsFile.writeInFile(totalMecCapacities+"\t");
				 resultsFile.writeInFile(resourcesUtilization+"\t");
				 resultsFile.writeInFile(results[1]+"\t");
				 resultsFile.writeInFile(results[0]+"\t");
				 resultsFile.writeInFile(admissionRate+"\t");
				 resultsFile.writeInFile(results[4]+"\t");
			}	 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
	//ResultsRun.run("bendersMasterIncumbent");
	ResultsRun.run("benders");
	//ResultsRun.run("LARAS_MIP");
	
	}

}
