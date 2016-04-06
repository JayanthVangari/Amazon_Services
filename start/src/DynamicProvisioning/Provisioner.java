package DynamicProvisioning;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
public class Provisioner implements Runnable
{
	private AmazonSQS sqs;
	String globaltaskqueue;
	static int minWorkers;
	static int maxWorkers;
	String requestid;
	public Provisioner() throws Exception
	{
		init();
		
	}
	
	public Provisioner(String requestid) throws Exception
	{
		this.requestid=requestid;
		
	}
	

	private void init() throws Exception {
		 AWSCredentials credentials = null;
		    
		    try {
		        credentials = new ProfileCredentialsProvider("default").getCredentials();
		    } catch (Exception e) {
		        throw new AmazonClientException(
		                "Cannot load the credentials from the credential profiles file. " +
		                "Please make sure that your credentials file is at the correct " +
		                "location (/home/jay2106/.aws/credentials), and is in valid format.",
		                e);
		    }

		    sqs = new AmazonSQSClient(credentials);
		    Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		    sqs.setRegion(usWest2);

}

	private void monitor() 
	{
		
		try
		{
			Requests r=new Requests();
			
			long temp=0,currentlength;
		
		
			GetQueueUrlRequest requestqueueUrl=new GetQueueUrlRequest("taskqueue");
			globaltaskqueue=sqs.getQueueUrl(requestqueueUrl).getQueueUrl();
		
		while(true)
		{
			currentlength=getlength();
			if(currentlength>temp)
			{
			String requestid=r.submitRequests();
				
				new Thread(new Provisioner(requestid)).start();
			}
				temp=currentlength;
		}
		}
			catch(QueueDoesNotExistException e)
			{
				monitor();
			}
			catch(Exception e)
		{
				e.printStackTrace();
		}
				
		
	}
	
	@Override
	public void run(){
		try
		{
			Requests r=new Requests();
			String ipaddress=r.waitUntilActiveandRunning(requestid);
			System.out.println("ipaddress :"+ipaddress);
			int exival=script("./workers.sh",ipaddress);
			
			//if(exival==0)
			//{	
				//script("./execWorker.sh",ipaddress);
			//}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		Thread.currentThread().interrupt();
		
	}
	private int script(String script, String ipaddress) throws IOException {
		
		int exival=2;
		while(exival!=0)
		{	
			String[] env = {"PATH=/bin:/usr/bin/"};
			String cmd =script+" "+ipaddress;
			Process process =Runtime.getRuntime().exec(cmd, env);
			try {
				exival=process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		//System.out.println("e "+exival);
		}
		return exival;
	}


	private long getlength() {
		
		List<String> attributeNames;
		attributeNames=new ArrayList<String>();
		attributeNames.add("ApproximateNumberOfMessages");
		GetQueueAttributesResult gr=sqs.getQueueAttributes(globaltaskqueue, attributeNames);
		Map<String,String> attr=gr.getAttributes();
		int queuelength=Integer.parseInt(attr.get("ApproximateNumberOfMessages"));
		
		return queuelength;
		
	}


	public static void main(String[] args) throws Exception
	{
	
		Provisioner p=new Provisioner();
		minWorkers=0;
		maxWorkers=Integer.valueOf(args[0]);
		p.monitor();
	}


	
}