To compile execute the script 'build.sh'

To run a Peer execute the script 'run-peer.sh' with the follwing arguments: run-peer.sh <version> <peerId>

To run the TestApp execute the script 'run-app.sh' with the follwing arguments for each opperation:
    - Backup: run-app.sh BACKUP/BACKUPENH <peerIdInitiator> <filePath> <replicationDegree>
    - Restore: run-app.sh RESTORE/RESTOREENH <peerIdInitiator> <filePath>
    - Delete: run-app.sh DELETE <peerIdInitiator> <filePath>
    - Space Reclaim: run-app.sh RECLAIM <peerIdInitiator> <diskSpace>
    - State: run-app.sh STATE <peerIdInitiator>