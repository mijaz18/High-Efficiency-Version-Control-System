# Low-Bandwidth-File-Syncing
– An API for syncing file directories and pushing file modifications to a main server in a highly efficient manner

– Useful when trying to sync files over low bandwidth networks where we would like to transfer as few bytes as possible

– Used Merkle Trees - a specialized data structure for data validation in distributed systems - to compare directories

– Designed a protocol to be used by the client and server to communicate during the file syncing/pushing process

– Used web sockets to implement a two way communication channel
