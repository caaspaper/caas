ssh -f elb01 "sh /import/elb/startLocalNodes.sh $1"
ssh -f elb02 "sh /import/elb/startLocalNodes.sh $1"
ssh -f elb03 "sh /import/elb/startLocalNodes.sh $1"
ssh -f elb04 "sh /import/elb/startLocalNodes.sh $1"
echo executed commands
echo $1
