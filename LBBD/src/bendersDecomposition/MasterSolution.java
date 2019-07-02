package bendersDecomposition;

import java.util.ArrayList;

import helperClasses.Output;

public class MasterSolution {

	public int A; // nb of apps
	public int U; // nb of tasks/users
	public int J; //
	public int P;
	public int [][][]beta_uajValues;
	public int [][]quaValues;
	public int [][]zapValues;
	public int[] naValues;
	public int[] pa;
	public double objective;
	
	public MasterSolution(int U, int A, int J, int P)
	{
		this.U=U;
		this.A=A;
		this.J=J;
		this.P =P;

		this.quaValues = new int [this.U][this.A];
		this.zapValues = new int [this.A][this.P];
		this.naValues = new int [this.A];
		this.pa = new int[this.A];
		this.beta_uajValues=new int [this.U][this.A][this.J];
		
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
	public String toString()
	{
		String st = "";

		// print results
		st += "\t RESULTS\n";
		
		st += String.format("\t\t Objective = %f\n", this.objective);
		
		st += "\t\t" + Output.printDoubleArray(this.quaValues, "qua[U][A]", "\t\t q");		
		st += "\t\t" + Output.printDoubleArray(this.zapValues, "zap[A][P]", "\t\t z");
		st += "\t\t" + Output.printArray(this.pa, "pa[A]", "\t\t p");
		st += "\t\t" + Output.printArray(this.naValues, "na[A]", "\t\t n");
		st += "\t\t" + Output.printTripleArray(this.beta_uajValues, "beta_uaj[U][A][J]", "\t\t beta_uaj");
		
		return st;
	}
}
