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
import java.util.Collection;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;

public class InlineGettingStartedCodeSampleApp {

 
    public static void main(String[] args) {
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

        // Create the AmazonEC2Client object so we can call various APIs.
        AmazonEC2 ec2 = new AmazonEC2Client(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        ec2.setRegion(usWest2);

        // Initializes a Spot Instance Request
        RequestSpotInstancesRequest requestRequest = new RequestSpotInstancesRequest();

        requestRequest.setRequestCredentials(credentials);
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
        Collection<String> securityGroups = new ArrayList<String>();
        securityGroups.add("launch-wizard-2");
        
        launchSpecification.setSecurityGroups(securityGroups);
        launchSpecification.withKeyName("cassandra");
        // Add the launch specifications to the request.
        requestRequest.setLaunchSpecification(launchSpecification);
        
        //============================================================================================//
        //=========================== Getting the Request ID from the Request ========================//
        //============================================================================================//

        // Call the RequestSpotInstance API.
        RequestSpotInstancesResult requestResult = ec2.requestSpotInstances(requestRequest);
        List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();

        // Setup an arraylist to collect all of the request ids we want to watch hit the running
        // state.
        ArrayList<String> spotInstanceRequestIds = new ArrayList<String>();

        // Add all of the request ids to the hashset, so we can determine when they hit the
        // active state.
        for (SpotInstanceRequest requestResponse : requestResponses) {
            System.out.println("Created Spot Request: "+requestResponse.getSpotInstanceRequestId());
            spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
        }

        //============================================================================================//
        //=========================== Determining the State of the Spot Request ======================//
        //============================================================================================//

        // Create a variable that will track whether there are any requests still in the open state.
        boolean anyOpen;
        boolean isRunning=false;

        // Initialize variables.
        ArrayList<String> instanceIds = new ArrayList<String>();
        List<Reservation> reservation=new ArrayList<Reservation>();
        List<Instance> ins=new ArrayList<Instance>();
        String instanceid = null;
        do {
            // Create the describeRequest with tall of the request id to monitor (e.g. that we started).
            DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
            describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);
           
            // Initialize the anyOpen variable to false ??? which assumes there are no requests open unless
            // we find one that is still open.
            anyOpen=false;

            try {
                // Retrieve all of the requests we want to monitor.
                DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
                List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

                // Look through each request and determine if they are all in the active state.
                for (SpotInstanceRequest describeResponse : describeResponses) {
                        // If the state is open, it hasn't changed since we attempted to request it.
                        // There is the potential for it to transition almost immediately to closed or
                        // cancelled so we compare against open instead of active.
                        if (describeResponse.getState().equals("active")) {
                        		System.out.println("active");
                        		anyOpen = true;
                        		 instanceid=describeResponse.getInstanceId();
                        		 System.out.println("instanceid:"+instanceid);
                                 // Add the instance id to the list we will eventually terminate.
                                 instanceIds.add(instanceid);
                            	 DescribeInstancesRequest req=new DescribeInstancesRequest();
                             	DescribeInstancesResult k=ec2.describeInstances(req);
                             	reservation=k.getReservations();
                             	for (Reservation i:reservation)
                             	{
                             		ins=i.getInstances();
                             		//Instance in=new Instance();
                             			
                             		for(Instance in:ins)
                             		{
                             			if(in.getInstanceId().equals(instanceid))
                             			{
                             				System.out.println("in:"+in.getInstanceId());
                             				while(true)
                             				{	
                             					String state=in.getState().toString();
                             					System.out.println(state);
                             					if(state.contains("running"))
                             					{	String ipaddress=in.getPublicIpAddress();
                             	        			System.out.println(ipaddress);
                             	        			break;
                             					}
                             					else
                             					{
                             						Thread.currentThread().sleep(10000);
                             					}
                             					
                             					}	
                             					
                             			}
                                     		
                             			
                             		}
                             	}
                             
                         
                            break;
                        }
                       
                }
            } catch (AmazonServiceException e) {
                // If we have an exception, ensure we don't break out of the loop.
                // This prevents the scenario where there was blip on the wire.
                anyOpen = true;
            } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        } while (!anyOpen);

        
	    //============================================================================================//
        //====================================== Canceling the Request ==============================//
        //============================================================================================//
/*
        try {
            // Cancel requests.
            CancelSpotInstanceRequestsRequest cancelRequest = new CancelSpotInstanceRequestsRequest(spotInstanceRequestIds);
            ec2.cancelSpotInstanceRequests(cancelRequest);
        } catch (AmazonServiceException e) {
         // Write out any exceptions that may have occurred.
            System.out.println("Error cancelling instances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }

        //============================================================================================//
        //=================================== Terminating any Instances ==============================//
        //============================================================================================//
        try {
            // Terminate instances.
            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceIds);
            ec2.terminateInstances(terminateRequest);
        } catch (AmazonServiceException e) {
            // Write out any exceptions that may have occurred.
           System.out.println("Error terminating instances");
            System.out.println("Caught Exception: " + e.getMessage());
           System.out.println("Reponse Status Code: " + e.getStatusCode());
           System.out.println("Error Code: " + e.getErrorCode());
           System.out.println("Request ID: " + e.getRequestId());
        }
*/
    }

}
