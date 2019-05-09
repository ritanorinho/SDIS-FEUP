#!/bin/bash

if [ "$2" = "RECLAIM" ]
then
    	java project/TestApp localhost:Peer$1 $2 $3
elif [ "$2" = "BACKUP" ]
then
	java project/TestApp localhost:Peer$1 $2 $3 $4
elif [ "$2" = "BACKUPENH" ]
then
	java project/TestApp localhost:Peer$1 $2 $3 $4
elif [ "$2" = "STATE" ]
then
	java project/TestApp localhost:Peer$1 $2
elif [ "$2" = "DELETE" ]
then
	java project/TestApp localhost:Peer$1 $2 $3
elif [ "$2" = "RESTORE" ]
then
	java project/TestApp localhost:Peer$1 $2 $3
elif [ "$2" = "DELETEENH" ]
then
	java project/TestApp localhost:Peer$1 $2 $3
elif [ "$2" = "RESTOREENH" ]
then
	java project/TestApp localhost:Peer$1 $2 $3
fi