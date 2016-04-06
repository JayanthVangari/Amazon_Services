package Worker;


import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
public class Dynamo{
	
	boolean checktasks(Table table,String taskid)
    	{    	
		Item item=table.getItem("taskid", taskid);
    	if(item!=null)
    	{
    		return true;
    	}
    return false;
    }
    
    void put(Table table,String taskid,String task)
    {
    	try
    	{
    		Item item=new Item().withKeyComponent("taskid", taskid).with("task", task);
    		table.putItem(item);
    	}
    	catch(Exception e)
    	{
    		System.err.println(e.getMessage());
    	}
    }
    
 	void delete(Table table, String taskid) {
 		try
 		{
 			
 			table.deleteItem("taskid", taskid);
 		}
 		catch(Exception e)
 		{
 			System.err.println(e.getMessage());
 		}
	}
 	
}
