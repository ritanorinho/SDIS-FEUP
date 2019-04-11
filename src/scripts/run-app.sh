#!/bin/bash

if [ "$2" = "RECLAIM" ]
then
    	java project/TestApp localhost:Peer$1 $2 $3
elif [ "$2" = "BACKUP" ]
then
	java project/TestApp localhost:Peer$1 $2 $3 $4
elif [ "$2" = "STATE" ]
then
	java project/TestApp localhost:Peer$1 $2
else
	java project/TestApp localhost:Peer$1 $2 $3

fi