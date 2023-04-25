package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    @FXML
    TextArea inputArea;

    @FXML
    Label currentUsername;

    @FXML
    Label currentOnlineCnt;

    @FXML
    ListView<Message> chatContentList;

    @FXML
    ListView<String> chatList;
    Socket socket = null;
    BufferedWriter bfwriter = null;
    BufferedReader bfreader = null;
    String username;
    HashMap<String, ArrayList<Message>> messages = new HashMap<>();
    String[] namelist = null;
    boolean refreshcheck = false;
    String currentgroup = null;
    ArrayList<String> grouplist = new ArrayList<>();
    boolean ingroupchat = false;
    boolean refreshgroup = false;
    ArrayList<String> groupmember = new ArrayList<>();

    @FXML
    public void shutdown(){
        try{
            this.socket.close();
        }catch (IOException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The server isn't start");
            alert.show();
        }
    }


    private void refreshnamelist(String names) throws IOException {
        if(names.equals("*")) this.namelist = new String[0];
        else this.namelist = names.split("/");
        refreshcheck = true;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                currentOnlineCnt.setText("Online: "+Integer.toString(namelist.length));
            }
        });
    }

    synchronized private void refreshgroupmembers(String members){
        System.out.println("get:"+members);
        String[] member = members.split("/");
        groupmember.clear();
        for(int i = 0;i < member.length;i++){
            groupmember.add(member[i]);
            System.out.println("member "+i+" is "+member[i]);
        }
        refreshgroup = true;
    }

    synchronized private void refreshchatlist() throws IOException, InterruptedException {
        if(currentgroup != null && currentgroup.contains("("))ingroupchat = true;
        else ingroupchat = false;
        chatList.getItems().clear();
        chatList.refresh();
        chatList.getItems().addAll(grouplist);
        if(ingroupchat){
            refreshgroup = false;
            bfwriter.write("G"+currentgroup);
            bfwriter.flush();
            wait(10);
            for(String s : groupmember){
                chatList.getItems().add("Chatting: "+s);
            }
        }
        chatList.refresh();
    }

    private boolean checkname(String name, String[] namelist){
        System.out.println("checking");
        for(int i = 0;i < namelist.length;i++){
            if(namelist[i].equals(name))return false;
        }

        for(int i = 0;i < name.length();i++){
            if((name.charAt(i) < 'a' || name.charAt(i) > 'z') &&
                    (name.charAt(i) < 'A' || name.charAt(i) > 'Z') &&
                    (name.charAt(i) < '0' || name.charAt(i) > '9') && name.charAt(i) != '-'){
                return false;
            }
        }
        return true;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            socket = new Socket("127.0.0.1",9999);
            Dialog<String> dialog = new TextInputDialog();
            dialog.setTitle("Login");
            dialog.setHeaderText(null);
            dialog.setContentText("Username:");
            //通过标准输入流获取字符流
            Optional<String> input = dialog.showAndWait();
            bfwriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            bfreader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            System.out.println("start to get namelist");
            bfwriter.write("F");
            bfwriter.flush();
            System.out.println("already write");
            char[] names = new char[105];
            bfreader.read(names);
            System.out.println("already read");
            this.namelist = String.valueOf(names).trim().substring(1).split("/");
            System.out.println("already get namelist");

            if (input.isPresent() && !input.get().isEmpty()) {
                username = input.get();
                System.out.println(username);
                while(!checkname(username, namelist)){
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText("Invalid name, please try again!");
                    alert.showAndWait();
                    input = dialog.showAndWait();
                    username = input.get();
                }
                String str = "S"+username;
                System.out.println("already get username");
                bfwriter.write(str);
                bfwriter.flush();
                System.out.println("Succesfully create a new user");
            } else {
                System.out.println("Invalid username " + input + ", exiting");
                Platform.exit();
            }
            currentUsername.setText("current user:"+username);
            chatContentList.setCellFactory(new MessageCellFactory());
            chatList.setCellFactory(new Messagegrouplist());
            work();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The Server is not started");
            alert.show();
            shutdown();
        }
    }

    public void work() throws IOException {
        System.out.println("Start Work");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readfromserver();
            }
        }).start();
    }

    public void addamessage(Message mes) throws IOException {
        ArrayList<Message> lv;
        if(mes.getSentBy().equals("System message")){
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        refreshchatlist();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if(mes.getSendTo().equals(username)){
                lv = messages.get(mes.getSentBy());
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if(currentgroup != null && currentgroup.equals(mes.getSentBy())){
                            chatContentList.getItems().add(mes);
                            chatContentList.refresh();
                        }
                    }
                });
        }else{
            lv = messages.get(mes.getSendTo());
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if(currentgroup != null && currentgroup.equals(mes.getSendTo())){
                        chatContentList.getItems().add(mes);
                        chatContentList.refresh();
                    }
                }
            });
        }
        lv.add(mes);
        System.out.println("Add message successfully:" + mes);
        for(int i = 0;i < lv.size();i++) {
            System.out.println(i + ":" + lv.get(i));
        }
    }

    public void readfromserver(){
        try{
            while(true){
                if(socket.isClosed()){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setContentText("The socket has been closed");
                        }
                    });
                    return;
                }
                char[] servermes = new char[505];
                bfreader.read(servermes);
                String instruct = String.valueOf(servermes).trim();
                System.out.println("Read instruct:" + instruct);
                if(instruct.length() <= 0){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setContentText("Disconnect with the server");
                            alert.show();
                        }
                    });
                    return;
                }
                instruct = instruct.substring(1);
                switch(servermes[0]){
                    case 'C':{
                        ArrayList<Message> lv = new ArrayList<>();
                        messages.put(instruct, lv);
                        final String groupname = instruct;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                grouplist.add(groupname);
                                try {
                                    refreshchatlist();
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    }
                    case 'M':{
                        Message mes = Message.getMessage(instruct);
                        addamessage(mes);
                        break;
                    }
                    case 'F':{
                        refreshnamelist(instruct);
                        break;
                    }
                    case 'E':{
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setContentText("User is offline");
                                alert.showAndWait();
                            }
                        });
                        break;
                    }
                    case 'G':{
                        refreshgroupmembers(instruct);
                    }
                }
            }
        }catch(IOException e){
            //e.printStackTrace();
            if(socket.isClosed()){
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("The socket has been closed");
                        alert.show();
                    }
                });
            }else{
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("The Server has been closed");
                        alert.show();
                    }
                });
            }
        }
    }

    @FXML
    public void createPrivateChat() throws IOException, InterruptedException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();


        System.out.println("Starting to create a private chat");
        refreshcheck = false;
        bfwriter.write("F");
        bfwriter.flush();

        while(!refreshcheck)continue;

        for(int i = 0;i < namelist.length;i++){
            System.out.println(namelist[i]);
        }
        for(int i = 0;i < namelist.length;i++){
            if(namelist[i].equals(username)){
                continue;
            }
            userSel.getItems().addAll(namelist[i]);
        }
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            if(user.get() != null && !user.equals("")){
                stage.close();
            }
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
        System.out.println(user.get());
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if(messages.get(user.get()) != null){
            currentgroup = user.get();
            chatContentList.getItems().clear();
            chatContentList.refresh();
            chatContentList.refresh();
            chatContentList.refresh();
            chatContentList.getItems().addAll(messages.get(user.get()));
            chatContentList.refresh();
            refreshchatlist();
        }else{
            ArrayList<Message> lv = new ArrayList<>();
            messages.put(user.get(), lv);
            System.out.println("Already send message");
            bfwriter.write("C"+username+"/"+user.get());
            bfwriter.flush();
            currentgroup = user.get();
            chatContentList.getItems().clear();
            chatContentList.refresh();
            grouplist.add(user.get());
            refreshchatlist();
        }

    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() throws IOException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        Button nextBtn = new Button("NEXT");
        Button okBtn = new Button("OK");
        ArrayList<String> users = new ArrayList<>();
        users.add(username);
        AtomicInteger cnt = new AtomicInteger(1);
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            if(user.get() == null || user.get().length() == 0)return;
            users.add(user.get());
            cnt.getAndIncrement();
            stage.close();
        });
        nextBtn.setOnAction(event -> {
            if(cnt.get() >= namelist.length-1){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("no more people");
                alert.showAndWait();
                return;
            }
            user.set(userSel.getSelectionModel().getSelectedItem());
            users.add(user.get());
            cnt.getAndIncrement();
            userSel.getItems().clear();
            for(int i = 0;i < namelist.length;i++){
                boolean tag = true;
                for(int j = 0;j < users.size();j++){
                    if(namelist[i].equals(users.get(j))){
                        tag = false;
                        break;
                    }
                }
                if(tag)userSel.getItems().add(namelist[i]);
            }
        });

        for(int i = 0;i < namelist.length;i++){
            boolean tag = true;
            for(int j = 0;j < users.size();j++){
                if(namelist[i].equals(users.get(j))){
                    tag = false;
                    break;
                }
            }
            if(tag)userSel.getItems().add(namelist[i]);
        }

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, nextBtn, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        users.sort(Comparator.naturalOrder());
        System.out.println(users);

        String groupname = "";
        String alluser = "";
        for(int i = 0;i < users.size();i++){
            if(i <= 2){
                groupname += ","+users.get(i);
            }
            alluser += "/"+users.get(i);
        }
        groupname = groupname.substring(1);
        if(users.size() > 3)groupname  += "...";
        groupname += "("+users.size()+")";
        System.out.println("Send: C"+groupname+alluser);
        bfwriter.write(("C"+groupname+alluser));
        bfwriter.flush();
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(currentgroup == null){
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setContentText("Please choose a private chat or a group chat");
                                alert.showAndWait();
                            }
                        });
                       return;
                    }
                    System.out.println("tag_read");
                    String str = inputArea.getText();
                    if(str == null || str.length() == 0){
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setContentText("Invalid messsage");
                                alert.showAndWait();
                            }
                        });
                        return;
                    }
                    BufferedWriter toserver = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    System.out.println("Already read");
                    inputArea.clear();
                    Message mes = new Message(System.currentTimeMillis(), username, currentgroup, str);
                    System.out.println(mes);
                    toserver.write("M" + mes.toString());
                    toserver.flush();

                }catch(IOException e){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("The Server has been closed");
                    alert.show();
                }
            }
        }).start();
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */

    private class Messagegrouplist implements Callback<ListView<String>, ListCell<String>> {
        @Override
        public ListCell<String> call(ListView<String> param) {
            return new ListCell<String>() {
                @Override
                public void updateItem(String groupname, boolean empty) {
                    super.updateItem(groupname, empty);
                    if (empty || Objects.isNull(groupname)) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                setText(null);
                            }
                        });
                        return;
                    }

                    setOnMouseClicked(event-> {
                                if (!isEmpty() && !(getText() == null)) {
                                    String g = getText();
                                    System.out.println("Click on " + g);
                                    System.out.println("current group is:"+currentgroup);
                                    if ( g != null && g.length() > 0 && !g.equals(currentgroup) && !g.contains("Chatting:")) {
                                        currentgroup = g;
                                        if(currentgroup.contains("("))ingroupchat = true;
                                        else ingroupchat = false;
                                        System.out.println("now group is:"+currentgroup);
                                        try {
                                            refreshchatlist();
                                        } catch (IOException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        chatContentList.getItems().clear();
                                        chatContentList.refresh();
                                        chatContentList.getItems().addAll(messages.get(g));
                                    }
                                }
                    });
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            setText(groupname);
                        }
                    });
                }

            };


        }
    }
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                            setGraphic(wrapper);
                }
            };
        }
    }
}
