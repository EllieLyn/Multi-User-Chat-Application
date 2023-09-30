package org.example;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ServerWorker is basically a thread, runs to handleClientSocket, communicate with client
public class ServerWorker extends Thread {
    private final Socket clientSocket;
    private OutputStream outputStream;
    private String login = null;
    private Server server;
    private Set<String> topicSet = new HashSet<>();

    public ServerWorker(Server server, Socket clientSocket){
        this.server = server;
        this.clientSocket = clientSocket;
    }
    @Override
    public void run(){
        try {
            handleClientSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // read data from the client, and also send data back to the client
    private void handleClientSocket() throws IOException, InterruptedException {
        // by using input stream and output stream, we have a bi-directional communication with the client connection
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
        // crate a bufferReader to read line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line=reader.readLine()) != null){
            String[] tokens = StringUtils.split(line);
            if(tokens != null && tokens.length > 0){
                String cmd = tokens[0];
                if("logoff".equals(cmd) || "quit".equalsIgnoreCase(cmd)){
                    handleLogoff();
                    break;
                } else if("msg".equalsIgnoreCase(cmd)) {
                    // instead of passing in tokens, we pass in a different version of token: msg user msg (if msg contains spaces/more than one word)
                    // i.e msg guest Hello World not working, this is being fixed
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                }else if("login".equalsIgnoreCase(cmd)){
                    handleLogin(outputStream, tokens);
                } else if("join".equalsIgnoreCase(cmd)){
                    handleJoin(tokens);
                } else if("leave".equalsIgnoreCase(cmd)){
                    handleLeave(tokens);
                } else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    public String getLogin(){
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        // not deal with storing username and password, so hardcode some users in the code
        if(tokens.length == 3){
            String login = tokens[1];
            String password = tokens[2];
            // take login and password of user input, check to check if login and password are correct
            // if yes, return ok login, else return error login
            if((login.equals("guest") && password.equals("guest")) || (login.equals("ellie") && password.equals("ellie"))){
                String msg = "ok login";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully: " + login);


                String onlineMsg = "online " + login + "\n";
                List<ServerWorker> workList = server.getWorkerList();
                // send other online users current user's status
                for(ServerWorker worker: workList){
                    if(!login.equals(worker.getLogin())){
                        worker.send(onlineMsg);
                    }
                }
                // send current user all other online logins
                for(ServerWorker worker: workList){
                    if(worker.getLogin() != null){
                        if(!login.equals(worker.getLogin())){
                            String msg2 = "online " + login + "\n";
                            send(msg2);
                        }
                    }
                }
            } else {
                String msg = "error login\n";
                outputStream.write(msg.getBytes());
                System.out.println("Login failed for " + login);
            }
        }
    }

    private void handleLogoff() throws IOException {
        // when logoff, remove self from list
        server.removeWorker(this);
        // send other online users current user's status
        String onlineMsg = "offline " + login + "\n";
        List<ServerWorker> workList = server.getWorkerList();
        for(ServerWorker worker: workList){
            if(!login.equals(worker.getLogin())){
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    // format: "msg" "login" body...
    // format: "msg" "#topic" body...
    public void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        // determine if it is topic by testing first character of sendTo to see if it is #
        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker: workerList){
            if(isTopic){
                if(worker.isMemberOfTopic(sendTo)){
                    String outMsg = "msg " + sendTo + ":" + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            } else {
                if(sendTo.equalsIgnoreCase(worker.getLogin())){
                    String outMsg = "msg " + login + body + "\n";
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleLeave(String[] tokens){
        if(tokens.length > 1){
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    public void handleJoin(String[] tokens){
        if(tokens.length > 1){
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    public boolean isMemberOfTopic(String topic){
        return topicSet.contains(topic);
    }

    public void send(String msg) throws IOException {
        if(login != null){
            outputStream.write(msg.getBytes());
        }
    }
}
