/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatinterface;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 *
 * @author arran
 */
public class ChatInterface extends JFrame {
    
    private Thread socketListener;
    private ServerSocket server;
    private Socket c = new Socket();
    private Thread messageListener;
    private String incoming;
    private DataOutputStream output;
    private DataInputStream input;
    private String clientName;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ChatInterface();
    }
    
    public ChatInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(490, 415);
        setTitle("Chat Zone");
        FlowLayout layout = new FlowLayout();
        layout.setVgap(20);
        setLayout(layout);
        
        
        JPanel headPanel = new JPanel();
        headPanel.setLayout(new BorderLayout());
        add(headPanel);
        
        JToggleButton host = new JToggleButton("Host Chat Server");
        
        JToggleButton join = new JToggleButton("Join Chat Server");
        
        JTextField ip = new JTextField("25.20.70.145");
        
        JSpinner port = new JSpinner(new SpinnerNumberModel(55555, 50000, 60000, 1));
        port.setEditor(new JSpinner.NumberEditor(port, "#"));
        
        JTextField name = new JTextField("arran");
        
        JPanel spec = new JPanel(new GridLayout(3, 2));
        spec.add(new JLabel("ip:", SwingConstants.RIGHT));
        spec.add(ip);
        spec.add(new JLabel("Port:", SwingConstants.RIGHT));
        spec.add(port);
        spec.add(new JLabel("Name:", SwingConstants.RIGHT));
        spec.add(name);
        
        headPanel.add(host, BorderLayout.LINE_START);
        headPanel.add(join, BorderLayout.CENTER);
        headPanel.add(spec, BorderLayout.LINE_END);
        
        
        JTextArea dialog = new JTextArea(10, 39);
        JScrollPane dialogPane = new JScrollPane(dialog);
        dialog.setEditable(false);
        dialog.setOpaque(false);
        dialogPane.setOpaque(false);
        dialogPane.getViewport().setOpaque(false);
        dialogPane.setBorder(null);
        dialog.setFont(new Font("Hiragino Sans", Font.PLAIN, 13));
        dialog.setLineWrap(true);
        dialog.setWrapStyleWord(true);
        add(dialogPane);
        dialog.setText("Welcome to the Chat Zone!");
        
        JTextField chatField = new JTextField(39);
        chatField.setText("Send a message...");
        add(chatField);
        
        setLocationRelativeTo(null);
        setVisible(true);
        
        host.addActionListener((ActionEvent e) -> {
            if(join.isSelected()) {
                ip.setEnabled(true);
                port.setEnabled(true);
                name.setEnabled(true);
                try {
                    c.close();
                } catch (IOException ex) {}
                join.setSelected(false);
            }
            socketListener = new Thread(() -> {
                try {
                    server = new ServerSocket((int) port.getValue());
                    c = server.accept();
                    output = new DataOutputStream(c.getOutputStream());
                    output.writeUTF(name.getText());
                    input = new DataInputStream(c.getInputStream());
                    clientName = input.readUTF();
                    dialog.append("\n" + clientName + " connected!");
                    messageListener.start();
                } catch(BindException ex) {
                    dialog.append("\nCouldn't bind ip and port.");
                    ip.setEnabled(true);
                    port.setEnabled(true);
                    name.setEnabled(true);
                    host.setSelected(false);
                } catch (IOException ex) {}
            });
            
            messageListener = new Thread(() -> {
                for(;;) {
                    try {
                        incoming = input.readUTF();
                        dialog.append("\n" + clientName + ": " + incoming);
                    } catch (IOException ex) {
                        try {
                            input.close();
                            output.close();
                            c.close();
                            server.close();
                        } catch (IOException ex1) {}
                        dialog.append("\n" + clientName + " disconnected.");
                        ip.setEnabled(true);
                        port.setEnabled(true);
                        name.setEnabled(true);
                        host.setSelected(false);
                        break;
                    }
                }
            });
            
            if(host.isSelected()) {
                dialog.append("\nHosting chat server on port " + (int) port.getValue() + " as '" + name.getText() + "'.");
                ip.setEnabled(false);
                port.setEnabled(false);
                name.setEnabled(false);
                socketListener.start();
            } else {
                ip.setEnabled(true);
                port.setEnabled(true);
                name.setEnabled(true);
                try {
                    dialog.append("\nChat server closed.");
                    c.close();
                    server.close();
                    messageListener.interrupt();
                    socketListener.interrupt();
                } catch (IOException ex) {}
            }
        });
        
        join.addActionListener((ActionEvent e) -> {
            if(host.isSelected()) {
                try {
                    c.close();
                    server.close();
                    messageListener.interrupt();
                    socketListener.interrupt();
                    dialog.append("\nChat server closed.");
                } catch (IOException ex) {}
                host.setSelected(false);
            }
            
            messageListener = new Thread(() -> {
                for(;;) {
                    try {
                        incoming = input.readUTF();
                        dialog.append("\n" + clientName + ": " + incoming);
                    } catch (IOException ex) {
                        try {
                            input.close();
                            output.close();
                            c.close();
                        } catch (IOException ex1) {}
                        dialog.append("\nDisconnected");
                        ip.setEnabled(true);
                        port.setEnabled(true);
                        name.setEnabled(true);
                        join.setSelected(false);
                        break;
                    }
                }
            });
            
            if(join.isSelected()) {
                dialog.append("\nAttempting to connect to " + ip.getText() + ":" + port.getValue() + " as '" + name.getText() + "'.");
                
                try {
                    c = new Socket(ip.getText(), (int) port.getValue());
                    input = new DataInputStream(c.getInputStream());
                    clientName = input.readUTF();
                    output = new DataOutputStream(c.getOutputStream());
                    output.writeUTF(name.getText());
                    dialog.append("\nConnected to " + clientName);
                    ip.setEnabled(false);
                    port.setEnabled(false);
                    name.setEnabled(false);
                    messageListener.start();
                } catch (IOException ex) {
                    dialog.append("\nCould not connect.");
                    join.setSelected(false);
                }
            } else {
                ip.setEnabled(true);
                port.setEnabled(true);
                name.setEnabled(true);
                try {
                    c.close();
                } catch (IOException ex) {}
            }
        });
        
        chatField.addActionListener((ActionEvent e) -> {
            if(!chatField.getText().equals("")) {
                dialog.append("\nYou: " + chatField.getText());
                try {
                    output.writeUTF(chatField.getText());
                } catch (NullPointerException | IOException ex) {
                    dialog.append("\n(You're the only one here!)");
                } 
                chatField.setText("");
            }
        });
        
        chatField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                chatField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
        
        name.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                name.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
    }
}