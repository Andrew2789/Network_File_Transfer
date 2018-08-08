# Network_File_Transfer  
A Java/JavaFX application that allows for direct network transfer of files and folders.  

This project is a work in progress - it is currently capable of sending folders to another instance of itself but the transferred files are not checked for integrity, and the protocol has no failsafes so it crashes occassionally.

Next steps:  
- Rework GUI so that connection interface is only shown before a connection is made, and so that it is more clear which fields need to be filled for a host or a client  
- Add murmur2 checksum to check integrity of received files, and implement failsafes in case of communication failures/file corruption
- Add support for encryption of file names and file contents
