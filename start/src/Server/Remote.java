package Server;

import java.net.*;
//import java.net.
import java.util.ArrayList;
import java.io.*;
import java.util.List;
import java.util.Map.Entry;

//import Worker.RemoteWorker;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;


public class Remote
{
	private int port;;
	private long taskcount;
	private ServerSocket ss;
	private Socket cli;
	public static AmazonSQS sqs;
	String requestqueueUrl;
	String responsequeueUrl;
	int max_tasks=10;
  
	public Remote(int port) throws Exception {
		this.port=port;
		init();
		}
	public Remote()
	{
	}
	private void init() throws Exception {
		ss=new ServerSocket(port);
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

		 		try {
		        // Create a queue
		        CreateQueueRequest requestqueue = new CreateQueueRequest("taskqueue");
		        requestqueueUrl = sqs.createQueue(requestqueue).getQueueUrl();
		        System.out.println("Created a new SQS queue called taskqueue.\n");
		        CreateQueueRequest responsequeue = new CreateQueueRequest("responsequeue");
		        responsequeueUrl = sqs.createQueue(responsequeue).getQueueUrl();
		        System.out.println("Created a new SQS queue called responsequeue.\n");

		        // List queues
		        System.out.println("Listing all queues in your account.\n");
		        for (String queueUrl : sqs.listQueues().getQueueUrls()) {
		            System.out.println("  QueueUrl: " + queueUrl);
		        }
		        		       
		    } catch (AmazonServiceException ase) {
		        System.out.println("Caught an AmazonServiceException, which means your request made it " +
		                "to Amazon SQS, but was rejected with an error response for some reason.");
		        System.out.println("Error Message:    " + ase.getMessage());
		        System.out.println("HTTP Status Code: " + ase.getStatusCode());
		        System.out.println("AWS Error Code:   " + ase.getErrorCode());
		        System.out.println("Error Type:       " + ase.getErrorType());
		        System.out.println("Request ID:       " + ase.getRequestId());
		    } catch (AmazonClientException ace) {
		        System.out.println("Caught an AmazonClientException, which means the client encountered " +
		                "a serious internal problem while trying to communicate with SQS, such as not " +
		                "being able to access the network.");
		        System.out.println("Error Message: " + ace.getMessage());
		    }
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
		List<String> tasksreceived= new ArrayList<String>();
		List<SendMessageBatchRequestEntry> tasks=new ArrayList<SendMessageBatchRequestEntry>();
		
		try
		{
			ObjectInputStream ois=new ObjectInputStream(cli.getInputStream());
			tasksreceived=(ArrayList<String>)ois.readObject();
			System.out.println("received");
			
			int i=0,j=0;
			taskcount=tasksreceived.size();
			for( String task: tasksreceived)
			{
				tasks.add(new SendMessageBatchRequestEntry(String.valueOf(j), task));
				i++;
				j++;
				if(i==max_tasks || j==tasksreceived.size())
				{	
					sqs.sendMessageBatch(requestqueueUrl, tasks);
					tasks.clear();
					i=0;
				}
				
			}
			submitresponse();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	
	}
	
	
	private void submitresponse() throws IOException
	{
		System.out.println("Waiting to Receive Response.\n");
	    
	ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(responsequeueUrl).withMaxNumberOfMessages(max_tasks);
	    List<Message> messages =null;
	    ArrayList<String> response=new ArrayList<String>();
	    int j=0;
	    while(j<taskcount)
	    {
	    	messages=sqs.receiveMessage(receiveMessageRequest).getMessages();
	    	j=j+messages.size();
		    if(!messages.isEmpty())
		    {	
	    		for(int i=0;i<messages.size();i++)
		    	{
	    			response.add(messages.get(i).getBody());
		    		String messageRecieptHandle = messages.get(i).getReceiptHandle();
		    		sqs.deleteMessage(new DeleteMessageRequest(responsequeueUrl, messageRecieptHandle));
		    	}
	    		
		    }   
		      
		 }
	    ObjectOutputStream os=new ObjectOutputStream(cli.getOutputStream());
    	os.writeObject(response);
    	
	    System.out.println("tasks sent to client :"+j);
	    System.out.println("done sending response to client.\n");
	    sqs.deleteQueue(new DeleteQueueRequest(responsequeueUrl));
	    sqs.deleteQueue(new DeleteQueueRequest(requestqueueUrl));
	    
	    
	   }
}