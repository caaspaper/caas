# kill local java
killall java

# kill java on remote elbs
for i in `seq -w 1 32`
do
echo elb${i}
ssh -f elb${i} "killall java"
done
