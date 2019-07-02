/**
 * this class represents the user in the subproblem (assigned by master to an application)
 */
package bendersDecomposition;

public class User {
	public int idInMaster;
	public int a; //id of app it is mapped to
	public int theta;//deadline
	public int mu;//cycles
	public int sigma;//arrival
	public double processingTime; //upper bound if 1.5 consider it 2 set based on pa - need it as double for the cut
	
	public int startSchedule; //when its schedule can start
	public boolean admitted;//if admitted

	
	public User (int idInMaster, int a, int theta, int mu, int sigma, double processingTime)
	{
		this.idInMaster = idInMaster;
		this.a = a;
		this.theta = theta;
		this.mu =mu;
		this.sigma = sigma;
		this.processingTime = processingTime;
		this.startSchedule =0;
		this.admitted =false;
	
	}
	
	
	public String toString()
	{
		String st="User ID:"+this.idInMaster;
		st +="\tApp: "+this.a+"\tCycles: "+this.mu+"\tArrival: "+this.sigma+"\tProcessing time: "+this.processingTime+"\tDeadline: "+this.theta+
				"\tAdmitted: "+this.admitted+"\tStartSchedule: "+this.startSchedule;
		return st;
	}
}
