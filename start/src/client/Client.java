package client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
public class Client
{
	String sched_ip,sched_port;
	static Socket client;

	public static void main(String[] args) throws Exception
	{
		Client c=new Client();
		if(args.length!=4)
		{
			
			System.out.println("Invalid arguments");
			System.out.println("Enter arguments with command -s IPADDRESS:PORT -w file ");
			System.exit(0);
		}
		String workfile=c.executeClient(args);
		//System.out.println(workfile);
		if(workfile!=null)
		{	
			c.shed_connect();
			int taskssize=c.sendtasks(workfile);
			c.readResponse(taskssize);
		}	
		return;
		
		}

	private void readResponse(int taskssize) throws Exception {
		int jobs=0;
		while(jobs<taskssize)
		{	
			ArrayList<String> response=new ArrayList<String>();
			ObjectInputStream is=new ObjectInputStream(client.getInputStream());
			response=(ArrayList<String>)is.readObject();
			for(String result:response)
			{
				System.out.println(result);
			}
			if(response!=null)
			{	
				jobs=jobs+response.size();
			
			}
		}
		System.out.println("number of response messages:"+jobs);
	}
	
	
	private  void shed_connect() throws NumberFormatException, UnknownHostException, IOException {
		System.out.println("connect attempt..");
		client=new Socket(sched_ip,Integer.parseInt(sched_port));
		System.out.println("connection established");
	}

	private int sendtasks(String workfile) throws Exception {
		ArrayList<String> tasks=new ArrayList<String>();
		ArrayList<String> sendtasks=new ArrayList<String>();
		String c="";
		String k="";
		int i=0,j;
		File f=new File(workfile);
			BufferedReader br=new BufferedReader(new FileReader(f));
			while((c=br.readLine())!=null)
			{
				//System.out.println(c);
				tasks.add(i,client.getLocalAddress()+":"+i+" "+c);
				i++;
				
			}
			j=0;i=0;
			while(j!=tasks.size())
			{	
				
				sendtasks.add(i,tasks.get(j));
				if(i==5 || j+1==tasks.size())
				{	
					ObjectOutputStream os=new ObjectOutputStream(client.getOutputStream());
					os.writeObject(tasks);
					i=0;		
				}
				i++;
				j++;
			}
			return tasks.size();
		}
		

	private String executeClient(String[] args) {
		if(!args[0].equals("-s"))
		{
			return null;
		}
		if(args[1].contains(":"))
			{
				String[] sched=args[1].split(":");
				sched_ip=sched[0];
				sched_port=sched[1];
			}
		if(!args[2].equals("-w"))
		{
			return null;
		}
		if(args[3]!=null)
		{
			File f=new File(args[3]);
			if(!f.exists())
			{
				System.out.println("workload file doesnot exist");
				return null;
			}
		}
		return args[3];
	}
	
}