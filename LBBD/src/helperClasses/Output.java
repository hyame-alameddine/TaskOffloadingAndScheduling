/**
 * This is helper class to output table and double matrice
 * All the functions are generic
 */
package helperClasses;
import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
public class Output {

	
	/**
	 * This function prints a one dimension array 
	 * @param t table to print
	 * @param s string to print (title)
	 * @param cplex if cplex is not null it will only print the value if it was not = 0
	 * 
	 * @return string with the info to print
	 * @throws IloException 
	 * @throws UnknownObjectException 
	 */
	public static <T> String printArray(T [] t, String s, String variable) throws UnknownObjectException, IloException
	{
		String st ="";
		st+=s+"\n";
		for(int i=0; i<t.length; i++)
		{ 	
			st+=String.format(variable+"["+i+"] ="+ t[i]+" \n");
		
		}
		st+="\n\n";
		
		return st;
	}
	
	
	/**
	 * This function prints a one dimension array 
	 * @param t table to print
	 * @param s string to print (title)
	 *  @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printArray(int [] t, String s, String variable)
	{
		String st ="";
		st+=s+"\n";
		for(int i=0; i<t.length; i++)
		{ 
			if (t[i]!=0)
			st+=String.format(variable+"["+i+"] = %4d \n", t[i]);
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a one dimension array 
	 * @param t table to print
	 * @param s string to print (title)
	 *  @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printArray(double [] t, String s, String variable)
	{
		String st ="";
		st+=s+"\n";
		for(int i=0; i<t.length; i++)
		{ 
			if (t[i]!=0)
			st+=String.format(variable+"["+i+"] = %4f \n", t[i]);
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a 1 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * 
	 * @return string with the info to print
	 */
	public static String printArray(IloNumVar []m, String s, String variable, IloCplex cplex) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
			
		for(int i=0; i<m.length; i++)
		{ 					
			try{
				
				if (cplex.getValue(m[i])!=0)
					st+=String.format(variable+"["+i+"]="+cplex.getValue(m[i])+"\n");
			}catch(IloException x) {}				
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 2 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * 
	 * @return string with the info to print
	 */
	public static <T> String printDoubleArray(T [][]m, String s, String variable) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
			
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				st+=String.format(variable+"["+i+"]["+j+"]= "+m[i][j]+"\n");
							
			}
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a 2 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * 
	 * @return string with the info to print
	 */
	public static String printDoubleArray(IloNumVar [][]m, String s, String variable, IloCplex cplex) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
			
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				try{
					
					if (cplex.getValue(m[i][j])!=0)
						st+=String.format(variable+"["+i+"]["+j+"]="+cplex.getValue(m[i][j])+"\n");
				}catch(IloException x) {}		
							
			}
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a 2 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * 
	 * @return string with the info to print
	 */
	public static String printDoubleArray(double [][]m, String s, String variable)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{
				if(m[i][j]!=0)
					st+=String.format(variable+"["+i+"]["+j+"]=%4f \n",m[i][j]);			
					
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 2 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printDoubleArray(int [][]m, String s, String variable)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
			
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{
				if(m[i][j] ==1)
					st+=String.format(variable+"["+i+"]["+j+"]=%4d \n",m[i][j]);			
					
			}
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a 3 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static <T> String printTripleArray(T [][][]m, String s, String variable) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{
					st+=String.format(variable+"["+i+"]["+j+"]["+k+"]="+m[i][j][k]+"\n");
					
				}			
			}
		}
		st+="\n\n";
	
		return st;
	}
	

	/**
	 * This function prints a 3 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printTripleArray(int [][][]m, String s, String variable) 
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{
					if(m[i][j][k] ==1)
					st+=String.format(variable+"["+i+"]["+j+"]["+k+"]="+m[i][j][k]+"\n");
					
				}			
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 3 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printTripleArray(double [][][]m, String s, String variable) 
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{
					if (m[i][j][k]!=0)
					{
						st+=String.format(variable+"["+i+"]["+j+"]["+k+"]="+m[i][j][k]+"\n");
					}
					
				}			
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 3 dimensions array list
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printTripleArrayList(ArrayList<ArrayList<ArrayList<Integer>>> P_uaj, String s, String variable) 
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<P_uaj.size(); i++)
		{ 	
			if(P_uaj.get(i) == null)
			{
				continue;
			}
			for(int j=0; j<P_uaj.get(i).size(); j++)
			{		
				
				if(P_uaj.get(i).get(j) == null)
				{
					continue;
				}
				
				for(int k=0; k<P_uaj.get(i).get(j).size(); k++)
				{
					
					st+=String.format(variable+"["+i+"]["+j+"]["+k+"]="+P_uaj.get(i).get(j).get(k)+"\n");
					
				}			
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	
	/**
	 * This function prints a 3 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable name of the variable to print (title)
	 * @param cplex if cplex is not null it will only print the value if it was not = 0
	 * 
	 * @return string with the info to print
	 */
	public static String printTripleArray(IloNumVar [][][]m, String s, String variable, IloCplex cplex) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					try{
						 if (m[i][j][k]!=null && cplex.getValue(m[i][j][k]) !=0)					
								st+=String.format(variable+"["+i+"]["+j+"]["+k+"]="+ cplex.getValue(m[i][j][k])+"\n");
					}catch(IloException x) {}					
				}			
			}
		}
		st+="\n\n";
		
		return st;
	}
	
	/**
	 * This function prints a 4 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * 
	 * @return string with the info to print
	 */
	public static <T> String printQuadrupleArray(T [][][][]m, String s, String variable) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					for(int l=0; l<m[i][j][k].length; l++)
					{						
						st+=String.format(variable+"["+i+"]["+j+"]["+k+"]["+l+"] =%4d \n",m[i][j][k][l]);
						
					}
				}	
			}
		}
		st+="\n\n";
		
		return st;
	}
	
	
	/**
	 * This function prints a 4 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @param cplex
	 * 
	 * @return string with the info to print
	 */
	public static  String printQuadrupleArray(IloNumVar[][][][]m, String s, String variable, IloCplex cplex) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					for(int l=0; l<m[i][j][k].length; l++)
					{	
						try{
							if( m[i][j][k][l]!=null && cplex.getValue(m[i][j][k][l])!=0)
								st+=String.format(variable+"["+i+"]["+j+"]["+k+"]["+l+"] ="+cplex.getValue(m[i][j][k][l])+"\n");
						}catch (IloException e){}	
						
					}
				}	
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 4 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static  String printQuadrupleArray(int [][][][]m, String s, String variable)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					for(int l=0; l<m[i][j][k].length; l++)
					{		if(m[i][j][k][l] !=0)			
							st+=String.format(variable+"["+i+"]["+j+"]["+k+"]["+l+"] =%4d \n",m[i][j][k][l]);
												
					}
				}	
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	
	/**
	 * This function prints a 4 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static  String printQuadrupleArray(double [][][][]m, String s, String variable)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
	
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					for(int l=0; l<m[i][j][k].length; l++)
					{		if(m[i][j][k][l] !=0)			
							st+=String.format(variable+"["+i+"]["+j+"]["+k+"]["+l+"] =%4f \n",m[i][j][k][l]);
												
					}
				}	
			}
		}
		st+="\n\n";
	
		return st;
	}
	
	/**
	 * This function prints a 5 dimensions array
	 * @param m array to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @param cplex
	 * 
	 * @return string with the info to print
	 */
	public static  String printCinqArray(IloNumVar[][][][][]m, String s, String variable, IloCplex cplex) throws UnknownObjectException, IloException
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
		for(int i=0; i<m.length; i++)
		{ 	
			for(int j=0; j<m[i].length; j++)
			{		
				for(int k=0; k<m[i][j].length; k++)
				{	
					for(int l=0; l<m[i][j][k].length; l++)
					{	
						for(int x=0; x<m[i][j][k][l].length; x++)
						{						
							try{
								if(cplex.getValue(m[i][j][k][l][x])!=0)
									st+=String.format(variable+"["+i+"]["+j+"]["+k+"]["+l+"]["+x+"]="+cplex.getValue(m[i][j][k][l][x])+"\n");
							}catch (IloException e){}	
							
						}
					}
				}	
			}
		}
		st+="\n\n";
	
		return st;
	}
	/**
	 * This function prints a 2 dimensions matrice
	 * @param m matrice to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printDoubleMatrice(int[][]m, String s)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
		
		if (m.length ==0) {
			return "Empty array";
		}
		//print column numbers
			
		for(int j=0; j<m[0].length; j++)
		{
			st+=j+" ";			
		}
		
		st+="\n-----------\n";
		
		for(int i=0; i<m.length; i++)
		{ 	
			//print row number
			st+=i+"| {";
		
			for(int j=0; j<m[i].length; j++)
			{		
				st+=m[i][j]+", ";			
			}
			
			st+="},\n\n";
		}
		
		return st;
	}
	
	
	/**
	 * This function prints a 2 dimensions matrice
	 * @param m matrice to print
	 * @param s string to print (title)
	 * @param variable, name of the variable to print
	 * @return string with the info to print
	 */
	public static String printDoubleMatrice(double[][]m, String s)
	{
		String st ="";
		
		st+="--"+s+"--\n   ";
		//print column numbers
			
		for(int j=0; j<m[0].length; j++)
		{
			st+=j+" ";			
		}
		
		st+="\n-----------\n";
		
		for(int i=0; i<m.length; i++)
		{ 	
			//print row number
			st+=i+"| {";
		
			for(int j=0; j<m[i].length; j++)
			{		
				st+=m[i][j]+", ";			
			}
			
			st+="},\n\n";
		}
		
		return st;
	}
	
	
	/**
	 * This function prints a table
	 * @param t table to print
	 * @param s string to print (title)
	 * 
	 * @return string with the info to print
	 */
	public static String printTable(int [] t, String s)
	{
		String st ="";
		st+="--"+s+"--\n";
	
		for(int i=0; i<t.length; i++)
		{ 	
			st+=i+"  ";			
		} 
		
		st+="\n-----------\n";
		for(int i=0; i<t.length; i++)
		{ 	
			st+=t[i]+", ";			
		}
		st+="\n\n";
		
		return st;
	}
	
	
	/**
	 * This function prints a table
	 * @param t table to print
	 * @param s string to print (title)
	 * 
	 * @return string with the info to print
	 */
	public static String printTable(double [] t, String s)
	{
		String st ="";
		st+="--"+s+"--\n";
	
		for(int i=0; i<t.length; i++)
		{ 	
			st+=i+"  ";			
		} 
		
		st+="\n-----------\n";
		for(int i=0; i<t.length; i++)
		{ 	
			st+=t[i]+", ";			
		}
		st+="\n\n";
		
		return st;
	}
}
