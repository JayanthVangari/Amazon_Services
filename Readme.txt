
1.Running the Scheduler:

The scheduler may run local workers or remote workers:

Format of work file:
Sleep 1000
Sleep 0
Sleep 10...

	1. Scheduler initiating local threads:
		There is only a single worker that initiates user specified number of threads. To run the worker on
		local machine number threads are specified as an argument.
			-s portnumber –lw <number_of_threads>

	2. Scheduler initiating Remote workers:
		The remote workers are generated based on the total time of sleep jobs. Each worker has
		internally 5 threads.If not specified default value is taken 3 minutes. To run the worker:
			-s portnumber –rw <time(Optional)>

2. Running the Client:

IP address and port number must be specified by checking the scheduler. To run the client:
	-s IP:PortNumber –w <Job_file_name>


>Start up EC2 instances for client , scheduler and Provisioner.
 Create DynamoDB table by executing TableCreate.jar.

Executing through Jars:

1.run Scheduler.jar 	: java -jar Scheduler.jar -s portnumber –lw <number_of_threads> -rw <time(optional)
2.run Provisioner.jar	: java -jar Provisioner.jar
3.run Client.jar 	: java -jar Client.jar -s IP:PortNumber –w <Job_file_name>


