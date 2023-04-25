package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.edu.sustech.cs209.chatting.common.Message;

public class Main {

  public static Map<String, serverThread> userlist = new HashMap<>();
  public static Map<String, ArrayList<serverThread>> grouplist = new HashMap<>();

  public static void main(String[] args) throws IOException {


    ServerSocket serverSocket = new ServerSocket(9999);
    System.out.println("start service");
    Thread t = new Thread(() -> {

      for (serverThread p : userlist.values()) {
        try {
          p.sock.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    Runtime.getRuntime().addShutdownHook(t);
    while (true) {
      Socket socket = serverSocket.accept();
      new serverThread(serverSocket, socket).start();
    }
  }

  public static String getuserlist() {
    String ret = "";
    for (String i : userlist.keySet()) {
      ret += "/";
      ret += i;
    }
    if (ret.length() > 0) ret = ret.substring(1);
    if (ret.length() == 0) ret = "*";
    System.out.println(ret);
    return ret;
  }

  public static String getgroupmember(String s) {
    String ret = "";
    if (grouplist.get(s) == null) return "";
    for (serverThread i : grouplist.get(s)) {
      ret += "/" + i.name;
    }
    ret = ret.substring(1);
    return ret;
  }


  private static class serverThread extends Thread {
    ServerSocket server = null;
    Socket sock = null;
    InputStream ins = null;
    OutputStream outs = null;
    String name = null;
    boolean available = true;


    public serverThread(ServerSocket s, Socket sock) {
      this.server = s;
      this.sock = sock;
    }

    public void quitchat() {
      try {
        this.available = false;
        for (String s : grouplist.keySet()) {
          boolean tag = false;
          for (int i = 0; i < grouplist.get(s).size(); i++) {
            serverThread p = grouplist.get(s).get(i);
            if (p.name.equals(name)) {
              grouplist.get(s).remove(p);
              tag = true;
              break;
            }
          }
//                    if(tag){
//                        System.out.println(s);
//                        for(serverThread p: grouplist.get(s)){
//                            System.out.println(p.name);
//                            Message mes = new Message(System.currentTimeMillis(), "System message", s, name+" has been offline");
//
//                            p.outs.write(("M"+mes.toString()+"\0").getBytes(StandardCharsets.UTF_8));
//                            for(int i = 1;i <= 100;i++){
//                                p.outs.flush();
//                            }
//                        }
//                    }
        }
        userlist.remove(name);
        System.out.println("Remove " + name + " succesfully");
        for (serverThread p : userlist.values()) {
          p.outs.write(("F" + getuserlist()).getBytes(StandardCharsets.UTF_8));
          for (int i = 1; i <= 20; i++) {
            p.outs.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void run() {
      try {
        ins = sock.getInputStream();
        outs = sock.getOutputStream();

        while (sock.isConnected() && available) {
          if (sock.isClosed()) available = false;
          char[] instruct = new char[505];
          Reader inr = new InputStreamReader(ins, StandardCharsets.UTF_8);
          inr.read(instruct);
          System.out.println("get instruct:" + String.valueOf(instruct).trim());
          int len = instruct.length;
          switch (instruct[0]) {
            case 'F': {
              this.outs.write(("F" + getuserlist()).getBytes(StandardCharsets.UTF_8));
              break;
            }
            case 'S': {
              String name = String.valueOf(instruct).trim();
              name = name.substring(1);
              this.name = name;
              userlist.put(name, this);
              String str = getuserlist();
              for (serverThread i : userlist.values()) {
                i.outs.write(("F" + str).getBytes(StandardCharsets.UTF_8));
                i.outs.flush();
              }
              break;
            }
            case 'C': {
              String create = String.valueOf(instruct).trim().substring(1);
              create = create.trim();
              String[] tu = create.split("/");
              String groupname = tu[0];
              if (tu.length == 2) {
                userlist.get(tu[1]).outs.write(("C" + groupname).getBytes(StandardCharsets.UTF_8));
                userlist.get(tu[1]).outs.flush();
              } else {
                ArrayList<serverThread> usersingroup = new ArrayList<>();
                for (int i = 1; i < tu.length; i++) {
                  usersingroup.add(userlist.get(tu[i]));
                  userlist.get(tu[i]).outs.write(("C" + groupname).getBytes(StandardCharsets.UTF_8));
                  userlist.get(tu[i]).outs.flush();
                }
                grouplist.put(groupname, usersingroup);
              }

              break;
            }
            case 'M': {
              Message mes = Message.getMessage(String.valueOf(instruct).trim().substring(1));
              String to = mes.getSendTo();
              if (userlist.get(to) != null) {
                userlist.get(to).outs.write(("M" + mes).getBytes(StandardCharsets.UTF_8));
                userlist.get(mes.getSentBy()).outs.write(("M" + mes).getBytes(StandardCharsets.UTF_8));
                userlist.get(to).outs.flush();
                userlist.get(mes.getSentBy()).outs.flush();
              } else if (grouplist.get(to) != null) {
                ArrayList<serverThread> users = grouplist.get(to);
                for (int i = 0; i < users.size(); i++) {
                  users.get(i).outs.write(("M" + mes).getBytes(StandardCharsets.UTF_8));
                  users.get(i).outs.flush();
                }
              } else {
                this.outs.write(("E").getBytes(StandardCharsets.UTF_8));
                outs.flush();
              }
              break;
            }
            case 'G': {
              String str = getgroupmember(String.valueOf(instruct).trim().substring(1));
              this.outs.write(("G" + str).getBytes(StandardCharsets.UTF_8));
              this.outs.flush();
              break;
            }
            case 'E': {
              available = false;
              break;
            }
          }
          outs.flush();
        }

      } catch (IOException e) {

      } finally {
        quitchat();
      }
    }
  }
}
