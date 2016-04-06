package DynamicProvisioning;
/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;

public class Requests {
    private AmazonEC2         ec2;
    private ArrayList<String> instanceIds;
    private ArrayList<String> spotInstanceRequestIds;

    public Requests () throws Exception {
        init();
    	
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

        ec2 = new AmazonEC2Client(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        ec2.setRegion(usWest2);
    }

    public String submitRequests() {
    	String request = null;
        //==========================================================================//
        //================= Submit a Spot Instance Request =====================//
        //==========================================================================//

        // Initializes a Spot Instance Request
        RequestSpotInstancesRequest requestRequest = new RequestSpotInstancesRequest();

        // Request 1 x t1.micro instance with a bid price of $0.03.
        requestRequest.setSpotPrice("0.05");
        requestRequest.setInstanceCount(Integer.valueOf(1));

        // Setup the specifications of the launch. This includes the instance type (e.g. t1.micro)
        // and the latest Amazon Linux AMI id available. Note, you should always use the latest
        // Amazon Linux AMI id or another of your choosing.
        LaunchSpecification launchSpecification = new LaunchSpecification();
        launchSpecification.setImageId("ami-d93622b8");
        launchSpecification.setInstanceType("t1.micro");

        // Add the security group to the request.
        ArrayList<String> securityGroups = new ArrayList<String>();
        
        securityGroups.add("launch-wizard-3");
        launchSpecification.setSecurityGroups(securityGroups);
        launchSpecification.setKeyName("cassandra");
        // Add the launch specifications to the request.
        requestRequest.setLaunchSpecification(launchSpecification);
       

        // Call the RequestSpotInstance API.
        RequestSpotInstancesResult requestResult = ec2.requestSpotInstances(requestRequest);
        
        List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();

        // Setup an arraylist to collect all of the request ids we want to watch hit the running
        // state.
        spotInstanceRequestIds = new ArrayList<String>();

        // Add all of the request ids to the hashset, so we can determine when they hit the
        // active state.
        for (SpotInstanceRequest requestResponse : requestResponses) {
            System.out.println("Created Spot Request: "+requestResponse.getSpotInstanceRequestId());
            request=requestResponse.getSpotInstanceRequestId();
            spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
            
            
        }
        return request;
    }

public String waitUntilActiveandRunning(String request) throws InterruptedException  {
	
		boolean isActive=false;
		boolean isRunning=false;
		List<Reservation> reservation=new ArrayList<Reservation>();
	    List<Instance> ins=new ArrayList<Instance>();
	    String instanceid = null;
	    String ipaddress=null;
	    while(!isActive)
        {	
        	DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
        	describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);
        try
        {
            // Retrieve all of the requests we want to monitor.
            DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
            List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

            for (SpotInstanceRequest describeResponse : describeResponses) {
            	if(describeResponse.getSpotInstanceRequestId().equalsIgnoreCase(request))
            	{
            		if (describeResponse.getState().equals("active")) {
            			System.out.println(describeResponse.getSpotInstanceRequestId()+" is active");
            			isActive=true;
            			instanceid=describeResponse.getInstanceId();
            			DescribeInstancesRequest req=new DescribeInstancesRequest().withInstanceIds(instanceid);
                        DescribeInstancesResult k=ec2.describeInstances(req);
                        reservation=k.getReservations();
                        for (Reservation i:reservation) {
                        	ins=i.getInstances();
                        	for(Instance in:ins) {
                        		
                        		if((in.getInstanceId()).equals(instanceid)) {	
                        			ipaddress=in.getPublicIpAddress();
                        			while(ipaddress==null)
                        			{
                        				Thread.currentThread().sleep(1000);
                        				ipaddress=in.getPublicIpAddress();
                        				
                        			}
                        		
                        		}
                        	}
                        }
                    }
                  }
                
                else
                {
                	Thread.currentThread().sleep(1000);	
                }

            }
        } catch (AmazonServiceException e) {
            // Print out the error.
            System.out.println("Error when calling describeSpotInstances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());

            // If we have an exception, ensure we don't break out of the loop.
            // This prevents the scenario where there was blip on the wire.
            }
       }
       return ipaddress;
     }

}

