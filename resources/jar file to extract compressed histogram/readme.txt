step 1: compile the java program first
	javac .\ExtractCDF.java
	
step 2: build a jar file and link the java.class
	jar -cvf extractCDF.jar .\ExtractCDF.class
	
step 3: to run-
	java -cp .\extractCDF.jar ExtractCDF .\DEVICE_UTILIZATION.dat LAZY_FCFS