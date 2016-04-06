package Server;


public class Scheduler
{
	int sched_port;
	int worker_threads;
	public static void main(String[] args)throws Exception 
	{
			Scheduler c=new Scheduler();
			if(args.length<3 || args.length>4 )
			{
				
				System.out.println("Invalid arguments");
				System.out.println("Enter arguments with command -s PORT -lw NUM -rw ");
				System.exit(0);
			}
			c.executesched(args);
				

	}

	private void executesched(String[] args) throws Exception {
		if(!args[0].equals("-s"))
		{
			return;
		}
		if(args[1]!=null)
		{
			sched_port=Integer.parseInt(args[1]);
		}
		if(args.length==4)
	 	{	
			if(args[2].equals("-lw") || args[3]!=null)
			{
				worker_threads=Integer.parseInt(args[3]);
				Local l=new Local(worker_threads,sched_port);
			}
			else
			{
				return;
			}
		}
		else if(args.length==3)
		{
			if(!args[2].equals("-rw"))
			{
				return;
			}
			else
			{
				Remote r=new Remote(sched_port);
			}
		}
	}
}