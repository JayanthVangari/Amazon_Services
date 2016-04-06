package Worker;
import java.net.*;

import Server.Local;
public class LocalWorker implements Runnable
{
	Local l=new Local();
	String taskid;
	String task;
	int worker_threads;
	public LocalWorker(int worker_threads) {
		this.worker_threads=worker_threads;
		
	}
	public void exec_wt()
	
	{
		for(int i=0;i<worker_threads;i++)
		{
				new Thread(new LocalWorker()).start();
		}
	}
	public LocalWorker() {
	}
	@Override
	public void run() {
		//System.out.println("thread started");
		String job="";
		while(true)
		{
			if(!Local.taskqueue.isEmpty())
			{	
				job=Local.taskqueue.poll();
				//System.out.println(job);
				String[] split=job.split(" ",2);
				taskid=split[0];
				task=split[1];
				String[] tasksplit=task.split(" ");
				try {
					Thread.currentThread();
					Thread.sleep(Long.parseLong(tasksplit[1]));
					Local.responsequeue.offer(taskid+": "+"1");
				} catch (Exception e) {
					Local.responsequeue.offer(taskid+": "+"0");
				}	
			}
			
		}
		
	}
	
}