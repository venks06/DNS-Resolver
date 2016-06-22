Libraries used
==============
dnsjava.jar

Implementation:
===============
1. Added the root server details in a file "roots"
2. In code, I am reading the file from the "roots" file
3. I implemented Iterative method to resolve host name. Iterating till AnswerSection length is more than 1. If I get CNAME, iterate again. Roundrobin: I created a file "roundrobin" with initial value '0', After every request, it's value is increased and storeed in the file, and the corresponding root server is picked for resolution. so, the values goes from 0 to 12 and comes back to 0, and so on.
4. I am sending a query with given type and checking if I get any records with the given type and storing the results in "mydig_output.txt"
5. I picked top 25 websites from Alexa, and used the code written for the 1st part to resolve the host names and calculated the time taken by the process. Calculated the average for 10 iterations for all the 25 websites. Using Excel, I drew the CDF which is add in the 
zip file.

Test:
=====
1. Use mydnsresolver.jar and run command "java -jar mydnsresolver.jar www.mit.edu"
2. Use mydig.jar and run command ""java -jar mydig.jar www.stonybrook.edu MX"
3. Add cdf.java file to the project and run it. You can see the output in the console.
