package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.security.PrivateKey;

public class MessagePane extends JPanel implements MessageListener {
    private final ChatClient client;
    private final String login;


    // create a document model (JList model), pass the list of model as part of constructor for constructing the JList
    private DefaultListModel<String> listModel = new DefaultListModel<>();

    // create a list to show current conversations
    private JList<String> messageList = new JList<>(listModel);
    private JTextField inputField = new JTextField();

    public MessagePane(ChatClient client, String login) {
        this.client = client;
        this.login = login;

        // add self as message listener
        client.addMessageListener(this);

        setLayout(new BorderLayout());
        add(new JScrollPane(messageList), BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        // create an action listener handle send message
        inputField.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // send message to the client, add text to conversation list
                try {
                    String text = inputField.getText();
                    client.msg(login, text);
                    listModel.addElement("You: " + text);
                    inputField.setText("");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    // create a line has login and message body, add to the listModel
    // when other user send you a message, this method handles it
    @Override
    public void onMessage(String fromLogin, String msgBody) {
        // filter to make sure the message is for the correct messagePane
        if(login.equalsIgnoreCase(fromLogin)){
            String line = fromLogin + ": " + msgBody;
            listModel.addElement(line);
        }
    }
}
