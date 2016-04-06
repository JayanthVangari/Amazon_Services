package Server;
import java.net.*;
import java.util.ArrayList;
import java.io.*;
import java.util.LinkedList;
import java.util.Queue;

import Worker.LocalWorker;

//import Worker.LocalWorker;
public class Local
{
	private int worker_threads;
	private int port;;
	long taskcount;
	private ServerSocket ss;
	private Socket cli;
	public static Queue<String> taskqueue=new LinkedList<String>();
	public static Queue<String> responsequeue=new LinkedList<String>();
	public Local(int worker_threads,int port) throws Exception {
		this.worker_threads=worker_threads;
		this.port=port;
		init();
		}
	public Local() {
			}
	private void init() throws Exception {
		ss=new ServerSocket(port);
		System.out.println("scheduler started");
		while(true)
		{
			Socket connection=ss.accept();
			if(connection!=null)
			{
				cli=connection;
				System.out.println("connected");
				readTasks();
			}
		}
	}
	
	private void readTasks()
	{
		System.out.println("ready to schedule tasks");
		ArrayList<String> tasks=new ArrayList<String>();
		try
		{
			ObjectInputStream ois=new ObjectInputStream(cli.getInputStream());
			tasks=(ArrayList<String>)ois.readObject();
			System.out.println("received");
			
			taskcount=tasks.size();
			//System.out.println("taskcount :"+ taskcount);
			for( String task: tasks)
			{
				//System.out.println(task);
				taskqueue.offer(task);
			}
			}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		submitresponse();
		
	}
	private void submitresponse()
	{
		LocalWorker w=new LocalWorker(worker_threads);
		w.exec_wt();
		String result="";
		long jobs=0;
		int i=0;
		ArrayList<String> response=new ArrayList<String>();
		try
		{
			while(jobs!=taskcount)
				{	
					
					if(responsequeue.isEmpty())
					{
						Thread.currentThread().sleep(1);
					}
					else
					{	result=responsequeue.poll();
						if(result!=null)
						{	
							jobs++;
							response.add(i,result);
							i++;
						}
						if(jobs%5==0 || jobs==taskcount)
						{	
							ObjectOutputStream os=new ObjectOutputStream(cli.getOutputStream());
							os.writeObject(response);
							response.clear();
							i=0;
						}
					}	
				}
				
				
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}