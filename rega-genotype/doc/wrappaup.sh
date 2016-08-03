# paup some times does not exit properlly that leavs PhyloCluasterAnalysis in an 
# inf loop. Adding this script to paup cmd should solve the problem.

exec $* &> /dev/null 
