package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// want to have a collection of workers, so that they can be iterated and send message from one connection to other connection
public class Server extends Thread {
    private final int serverPort;
    private List<ServerWorker> workerList = new ArrayList<>();
    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    // serverWorker to access other serverWorkers
    public List<ServerWorker> getWorkerList(){
        return workerList;
    }

    @Override
    public void run(){
        try{
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true){
                System.out.println("About to accept client connection...");
                // clientSocket represents connection to the client, call accept in a
                // loop because it needs to continuously accept the connection from the client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                // test for clientSocket, every socket has output stream

                ServerWorker worker = new ServerWorker(this, clientSocket);
                // since ServerWorker extends thread, worker.start will directly execute the run() method of the ServerWorker class
                workerList.add(worker);
                worker.start();
                // create a new thread everytime we get a connection from the client
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void removeWorker(ServerWorker serverWorker){
        workerList.remove(serverWorker);
    }
}
