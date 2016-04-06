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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;

public class SecGroupCreate {

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
        
        // Create a new security group.
        try {
            CreateSecurityGroupRequest securityGroupRequest = new CreateSecurityGroupRequest(
                    "launch-wizard-3", "launch-wizard-3");
            CreateSecurityGroupResult result = ec2
                    .createSecurityGroup(securityGroupRequest);
            System.out.println(String.format("Security group created: [%s]",
                    result.getGroupId()));
        } catch (AmazonServiceException ase) {
            // Likely this means that the group is already created, so ignore.
            System.out.println(ase.getMessage());
        }

        String ipAddr = "0.0.0.0/0";
        
        // Create a range that you would like to populate.
        List<String> ipRanges = Collections.singletonList(ipAddr);
        
        List<IpPermission> ipPermission=new ArrayList<IpPermission>();
        ipPermission.add(new IpPermission().withIpProtocol("tcp")
                .withFromPort(new Integer(0))
                .withToPort(new Integer(65535))
                .withIpRanges(ipRanges)
        );
        ipPermission.add(new IpPermission().withIpProtocol("tcp")
                .withFromPort(new Integer(22))
                .withToPort(new Integer(22))
                .withIpRanges(ipRanges)
        );
        ipPermission.add(new IpPermission().withIpProtocol("udp")
                .withFromPort(new Integer(0))
                .withToPort(new Integer(65535))
                .withIpRanges(ipRanges)
        );
        
        // Open up port 23 for TCP traffic to the associated IP from above (e.g. ssh traffic).
       // IpPermission ipPermission = new IpPermission()
                
        //ipPermission.
        List<IpPermission> ipPermissions = new ArrayList<IpPermission>(ipPermission);

        try {
            // Authorize the ports to the used.
            AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest(
                    "launch-wizard-3", ipPermissions);
            ec2.authorizeSecurityGroupIngress(ingressRequest);
            System.out.println(String.format("Ingress port authroized: [%s]",
                    ipPermissions.toString()));
        } catch (AmazonServiceException ase) {
            // Ignore because this likely means the zone has already been authorized.
            System.out.println(ase.getMessage());
        }
    }

}
