COMPILE
> javac *.java

START RMI
> rmiregistry

RUN PEER
>java Peer <version> <n_peers> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>

RUN APPLICATION
- CHUNK BACKUP PROTOCOL
>java Application <host>/<peer_access_point> BACKUP <file_path> <desired_replication_degree>

- CHUNK RESTORE PROTOCOL
>java Application <host>/<peer_access_point> RESTORE <file_path>

- FILE DELETION SUBPROTOCOL
>java Application <host>/<peer_access_point> DELETE <file_path>

- SPACE RECLAIM SUBPROTOCOL
>java Application <host>/<peer_access_point> RECLAIM <max_amount_disk_space>

- STATE
>java Application <host>/<peer_access_point> STATE
