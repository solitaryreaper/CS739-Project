#/bin/sh!
while true;
do
echo `date` >> ./logs;
echo "**********************" >> ./logs;
java -jar statsHandler.jar >> ./logs
sleep 10;
done
