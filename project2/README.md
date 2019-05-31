
# Manual de utilização
Para testar o projeto há três passos fundamentais:
   
    * Iniciar os servidores
    * Iniciar os peers
    * Realizar pedidos aos peers

    Cada servidor pode ser inicializado da seguinte forma: java app/Server <serverPort> (<address> <port>)*
    A primeira porta corresponde à sua própria porta onde os peers e os outros servidores se vão conectar e os pares (address, port) correspondem a outros servidores. Aceita-se um número variável de servidores.

    Cada peer pode ser inicializado da seguinte forma: java app/Peer <peerId>  <accessPoint> <peerPort> (<address> <port>)+
    A primeira porta corresponde àquela através da qual se vai ligar ao servidor e aos outros peers, assim que necessário. O servidor principal é o primeiro par (address, port) . Neste caso é exigido que seja introduzido pelo menos um servidor.

    Por fim, para executar os protocolos a estrutura varia, sendo clarificadas na seguinte lista.

   * BACKUP: java app/TestApp host:<peerAccessPoint> BACKUP <filename> <repDegree>
   * RESTORE: java app/TestApp host:<peerAccessPoint> RESTORE <filename>
   * DELETE: java app/TestApp host:<peerAccessPoint> DELETE <filename>
   * RECLAIM: java app/TestApp host:<peerAccessPoint> RECLAIM <newAvailableSpace>