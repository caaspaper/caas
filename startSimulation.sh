time=$(date +%H.%I.%S)
echo $time
ssh -f elb01 "sh /import/elb/startLocalNodes.sh $1 >> log_$time.txt"
ssh -f elb02 "sh /import/elb/startLocalNodes.sh $1 >> log_$time.txt"
ssh -f elb01 "sh /import/elb/startLocalNodes.sh $1 >> log_$time.txt"
ssh -f elb04 "sh /import/elb/startLocalNodes.sh $1 >> log_$time.txt"

