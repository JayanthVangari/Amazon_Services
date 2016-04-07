# Amazon_Services

The project aims to make use of various Amazon AWS services : EC2, SQS and DynamoDB. It involves implementing a dynamic Provisioner that launches workers by issuing spot requests to execute tasks ( the tasks here are sleep tasks) submitted by client through a scheduler.

The scheduler uses Amazon SQS to insert tasks which are picked up by workers and It essentially is used to handle queue of requests to load balance across multiple workers. DynamoDB is used to avoid picking up Duplicate tasks from SQS.


