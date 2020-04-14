COMPILE
> javac *.java

START RMI
> rmiregistry

RUN PEER
>java Peer <version> <n_peers> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>
Eg: java Peer 1 5 224.0.0.1 9999 224.0.0.2 9998 224.0.0.3 9997

RUN APPLICATION
- CHUNK BACKUP PROTOCOL
>java Application <host>/<peer_access_point> BACKUP <file_path> <desired_replication_degree>
Eg: java Application localhost/1 BACKUP image.png 2

- CHUNK RESTORE PROTOCOL
>java Application <host>/<peer_access_point> RESTORE <file_path>
Eg: java Application localhost/1 RESTORE image.png

- FILE DELETION SUBPROTOCOL
>java Application <host>/<peer_access_point> DELETE <file_path>
Eg: java Application localhost/1 DELETE image.png

- SPACE RECLAIM SUBPROTOCOL
>java Application <host>/<peer_access_point> RECLAIM <max_amount_disk_space>
Eg: java Application localhost/1 RECLAIM 0

- STATE
>java Application <host>/<peer_access_point> STATE
Eg: java Application localhost/1 STATE 0
