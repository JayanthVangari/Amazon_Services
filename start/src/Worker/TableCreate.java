package Worker;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class TableCreate
{
	private static AmazonSQS sqs;
	private static AmazonDynamoDBClient dynamoDB;
	static String tableName="TasksVerification";
	private static DynamoDB db;
    
	public static void main(String[] args) throws Exception
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
			dynamoDB = new AmazonDynamoDBClient(credentials);
	        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
	        dynamoDB.setRegion(usWest2);
	        db=new DynamoDB(dynamoDB); 
	        CreateTableRequest createTableRequest;
	        
	        try {
	            System.out.println("checking if Table " +"'"+ tableName +"' EXISTS");
	             TableUtils.waitUntilExists(dynamoDB, tableName, 2000, 500);
	             System.out.println(tableName +" already EXISTS");
	        }
	        catch(AmazonClientException e)
	        {
	        		// Create a table with a primary hash key named 'name', which holds a string
	                createTableRequest = new CreateTableRequest().withTableName(tableName)
	                    .withKeySchema(new KeySchemaElement().withAttributeName("taskid").withKeyType(KeyType.HASH))
	                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("taskid").withAttributeType("S"))
	                    .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
	                    TableDescription createdTableDescription = dynamoDB.createTable(createTableRequest).getTableDescription();
	                System.out.println("Created Table: " + createdTableDescription);

	                // Wait for it to become active
	                System.out.println("Waiting for " + tableName + " to become ACTIVE...");
	                TableUtils.waitUntilActive(dynamoDB, tableName);
	                System.out.println(tableName+ " is ACTIVE");
	         }
	        
	
	}
}