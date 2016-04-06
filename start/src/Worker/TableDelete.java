package Worker;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;

import Worker.TableCreate;

public class TableDelete
{
	private static DynamoDB db;
	private static AmazonDynamoDBClient dynamoDB;
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
        dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDB.setRegion(usWest2);
		db=new DynamoDB(dynamoDB);
		Table t=db.getTable(TableCreate.tableName);
		t.delete();
		System.out.println("Table deleted");
		db.shutdown();
	}
}