package Worker;
import java.util.ArrayList;
import java.util.Collection;
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
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
public class RemoteWorker
{
	
	private static int max_idletime;
	private static AmazonSQS sqs;
	static String requestqueueUrl;
	private static Dynamo dyanmo;
	private static AmazonDynamoDBClient dynamoDB;
	static String responsequeueUrl;
	private static int max_tasks=10;
	static DynamoDB db;
	private AmazonEC2 ec2; 
	public static void main(String[] args) throws Exception
	{
		RemoteWorker r=new RemoteWorker();
		if(args.length>0)
		{
			if(!args[0].equals("-i"))
			{
				return;
			}
			if(args[1]!=null)
			{
				max_idletime=Integer.parseInt(args[1]);
			}
			else
			{
				System.out.println("Invalid arguments");
				System.out.println("Enter arguments with command -i idletime(in secs) ");
				return;
			}
		}
		else
		{
			System.out.println("Invalid arguments");
			System.out.println("Enter arguments with command -i idletime(in secs) ");
			return;
		}
		r.init();
		r.pullTasks();
	}
	
	private void init() throws Exception
	{
	
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
		dynamoDB = new AmazonDynamoDBClient(credentials);
        //Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDB.setRegion(usWest2);
        db=new DynamoDB(dynamoDB); 
        
		System.out.println("picking tasks from taskqueue.\n");
		GetQueueUrlRequest requestqueue = new GetQueueUrlRequest("taskqueue");
		requestqueueUrl = sqs.getQueueUrl(requestqueue).getQueueUrl();
		GetQueueUrlRequest responsequeue = new GetQueueUrlRequest("responsequeue");
		responsequeueUrl = sqs.getQueueUrl(responsequeue).getQueueUrl();
		
	}
        
	private void pullTasks() throws NumberFormatException, InterruptedException
	{
		long temp=0;
		long prevtime=0;
		long  idletime=0;
		int jobs=0;
		String result;
		List<String> attributeNames;
		String message,task,taskid;
		String receiptHandle;
		Table table= db.getTable(TableCreate.tableName);
		dyanmo=new Dynamo();
		attributeNames=new ArrayList<String>();
		attributeNames.add("ApproximateNumberOfMessages");
		
		GetQueueAttributesResult gr=sqs.getQueueAttributes(requestqueueUrl, attributeNames);
		Map<String,String> attr=gr.getAttributes();
		int queuelength=Integer.parseInt(attr.get("ApproximateNumberOfMessages"));
		System.out.println("sqs length: "+queuelength);
		
		while(true)
		{
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(requestqueueUrl).withMaxNumberOfMessages(max_tasks);
			List<Message> messages =null;
			
			messages=sqs.receiveMessage(receiveMessageRequest).getMessages();
			
			if((idletime/1000)>=max_idletime && max_idletime!=0) 
			{
				terminateWorkers();
				return;
			}
			if(!messages.isEmpty())
			{
				idletime=0;
				for( int i=0;i<messages.size();i++)
				{
					
					message=messages.get(i).getBody();
					receiptHandle=messages.get(i).getReceiptHandle();
					sqs.deleteMessage(requestqueueUrl,receiptHandle);
					String[] split=message.split(" ",2);
					taskid=split[0];
					task=split[1];
					if(dyanmo.checktasks(table,taskid)==false)
					{
						dyanmo.put(table, taskid, task);
						result=taskid+":"+executetask(task);
						//System.out.println("result:		"+result);
						dyanmo.delete(table, taskid);
						sqs.sendMessage(responsequeueUrl,result);
					}
					i++;
				}
			}
			else
			{
				temp=System.currentTimeMillis();
				if(prevtime!=0)
					idletime = idletime+temp-prevtime;
				prevtime=temp;
			}
		}
}

	private void terminateWorkers() {
		
		String instanceid=EC2MetadataUtils.getInstanceId();
		TerminateInstancesRequest te=new TerminateInstancesRequest();
		ec2.terminateInstances(te.withInstanceIds(instanceid));
		SpotInstanceRequest s=new SpotInstanceRequest().withInstanceId(instanceid);
		CancelSpotInstanceRequestsRequest cancelRequest = new CancelSpotInstanceRequestsRequest();
		cancelRequest.withSpotInstanceRequestIds(s.getSpotInstanceRequestId());
	    ec2.cancelSpotInstanceRequests(cancelRequest);
		
	}

	private int executetask(String task) throws NumberFormatException, InterruptedException {
		int result;
		try
		{
			String split[]=task.split(" ");
			Thread.currentThread();
			Thread.sleep(Long.parseLong(split[1]));
			result=1;
			
		}
		catch(Exception e)
		{
			result=0;
		}
		return result;
	}	
}	