package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    Double[] navigablearray;
    static Integer sequencenumber=0;
    static Integer size=5;
    static Double proposed_seen_sofar=0.0;
    static Integer key=0;
    static Double agreedpriority = 0.0;
    static long starttime=0l;

    BlockingQueue<Message> pq = new PriorityBlockingQueue(1000,new MessageComparator());
    Queue<String> delivery = new LinkedList<String>();
    ConcurrentSkipListMap<Double,String> hold_back = new ConcurrentSkipListMap<Double,String>();

    /* To get port number of this device*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final  String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        final EditText editText1 = (EditText) findViewById(R.id.editText1);
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        

        final Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText1.getText().toString() + "\n";
                editText1.setText(""); // This is one way to reset the input box.
                tv.append("\t" + msg); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                // return true;
            }
        });

    }

    //adding now
    class Message  {
        String name;

        Double priority;

        Message(String str, Double pri) {
            name = str;
            priority = pri;
        }

      }

    class MessageComparator implements Comparator<Message> {
        public int compare(Message msg1, Message msg2) {
            return msg1.priority.compareTo(msg2.priority);
        }
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);  //check this plz
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final  String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        private final ContentResolver amContentResolver=getContentResolver();
        private Uri amUri;

        //static Integer sequencenumber=0;

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }


        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            amUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            ServerSocket serverSocket = sockets[0];
            ConcurrentHashMap<String, List<Double>> hm=new ConcurrentHashMap<String, List<Double>>();


            while(true){
                try {
                    FIFO_Total fts=new FIFO_Total();


                    Socket clientSocket = serverSocket.accept();

                    BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String senddata = br.readLine();



                    //check if received message is proposed priority or first time message
                    if("pp".equals(senddata.substring(0,2))){
                        Log.d(TAG, "proposed priority has been sent");
                        //this message is proposed priority
                        //message is made as key and all proposed priorities are stored as values in arraylist

                        String msg=senddata.substring(senddata.lastIndexOf(",")+1);
                        List<Double> all_priority_values = hm.get(msg);
                        if(all_priority_values==null){
                           // List<Integer> all_priority_values;
                            all_priority_values = Collections.synchronizedList(new ArrayList<Double>());
                           // all_priority_values.add(Integer.parseInt(senddata.substring(9,senddata.lastIndexOf(","))));
                            hm.put(msg,all_priority_values);
                            starttime=System.currentTimeMillis();

                            Log.d(TAG,"start time is"+msg +" "+starttime);
                        }
                        String proposed_pri=senddata.substring(6,senddata.lastIndexOf(","))+senddata.substring(3,5);

                        all_priority_values.add(Double.parseDouble(proposed_pri.trim()));
                      // }
                        Log.d(TAG, "Number of values in hashmap is"+hm.size());
                        Log.d(TAG, "Number of values in hashmap arraylist is"+all_priority_values.size());


                        //check if proposals from all devices have arrived i.e., 5
                        if( hm.get(msg).size()==5){ //will have to ask TA and change
                            long stoptime=System.currentTimeMillis();
                            Log.d(TAG,"stop time is"+" "+msg+" "+stoptime);
                            Double agreedpriority= Collections.max(hm.get(msg)) ;
                            Log.d(TAG, "agreed priority is"+ msg+agreedpriority);
                            Log.d(TAG, "before sending agreed priority"+ msg);
                            fts.send_message_with_agreedpriority(msg, agreedpriority);
                        }
                    }

                    else if(senddata.substring(6,8).equals("fm")) {

                        String onlymessage=senddata.substring(senddata.lastIndexOf(",")+1);//check this
                        String message_sent_by_port=senddata.substring(0,5); //check this
                       // Log.d(TAG, "Size of pq after msg has been added is"+onlymessage+pq.size());
                        //Propose a sequence number which will be 1+sequencenumber and piggyback with pid and also message and send it
                        sequencenumber+=2;

                        //Double proposed_seen_sofar=0.0;//changenow
                        String proposed_sequence_number=myPort.substring(3)+","+Double.toString(Math.max(sequencenumber,agreedpriority)+1)+","+onlymessage;
                        Log.d(TAG, "seq no is"+sequencenumber);
                        Log.d(TAG, "aftre seq no is"+agreedpriority);
                        //sequencenumber=Math.max(sequencenumber,agreedpriority)+1;
                        Log.d(TAG, "value of proposed sequence number is"+proposed_sequence_number);

                        hold_back.put(Double.parseDouble(sequencenumber+"."+myPort),onlymessage+"-1");

                        Log.d(TAG, "added to hold_back first time" + onlymessage + "-1");
                        fts.send_proposed_priority(message_sent_by_port, proposed_sequence_number);
                    }
                    else { //when message  comes with agreed priority
                        String msg = senddata.substring(3, senddata.lastIndexOf(","));
                        agreedpriority =Double.parseDouble(senddata.substring(senddata.lastIndexOf(",") + 1));
                        Log.d(TAG, "in 3rd part"+msg);

                        NavigableSet navigableKeySet = hold_back.keySet();
                        for(Iterator iterator = navigableKeySet.iterator();iterator.hasNext();)
                        {

                            Double oldkey=(Double)iterator.next();
                            String comparemsg=hold_back.get(oldkey);//chaging to agreedpriority
                            if((comparemsg).equals(msg+"-1")){
                                Log.d(TAG, "changing to agreed priority"+ msg+" "+agreedpriority);
                                String s=hold_back.put(agreedpriority, comparemsg.substring(0, comparemsg.length() - 2));
                                hold_back.remove(oldkey);
                                if(s==null){
                                    Log.d(TAG,"added to hold back"+msg);
                                }


                            }

                        }
                       while ((!hold_back.isEmpty()) && !hold_back.get(hold_back.firstKey()).substring(hold_back.get(hold_back.firstKey()).length()-2).equals("-1")){//priority has been changed to agreed
                            Log.d(TAG, "adding to delivery"+hold_back.get(hold_back.firstKey()));
                            delivery.add(hold_back.get(hold_back.firstKey()));
                            hold_back.remove(hold_back.firstKey());

                            Log.d(TAG, "size of hold back is"+hold_back.size());

                        }

                    }




                    while(delivery.size()>=1) {
                        String finalmsg = delivery.remove();
                        Log.d(TAG, "finalmsg is" + finalmsg);

                        ContentValues val = new ContentValues();
                        val.put("key", key); // may have to change this
                        val.put("value", finalmsg.trim());
                        amUri = getContentResolver().insert(amUri, val);
                        publishProgress(key + finalmsg);
                        key += 1;
                    }






                                           //}
                }  catch(IOException e){
                    Log.e(TAG, "Accept Failed"+ e.getMessage());
                }


            }


        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");


            String filename = "GroupMessengerOutput2";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            //return;
        }
    }
// to display


    //@Override
    private class ClientTask extends AsyncTask<String, Void, Void> {

        protected Void  doInBackground(String... msgs) {
            //FIFO_Total ft=new FIFO_Total();
            //ft.sendmessagefirsttime(msgs);
            try {
                //String remotePort;// = REMOTE_PORT0;
                //  if (msgs[1].equals(REMOTE_PORT0)) {

                String msgToSend = msgs[0];
                String msguniqueid = msgs[1] +","+"fm,"+ msgToSend;
                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT0));
                PrintWriter out = new PrintWriter(socket0.getOutputStream(), true);
                out.println(msguniqueid);//send data to server
                socket0.close();

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT1));


                //my code
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msguniqueid);//send data to server
                socket.close();

                Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT2));
                out = new PrintWriter(socket1.getOutputStream(), true);
                out.println(msguniqueid);//send data to server
                socket1.close();

                Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT3));
                out = new PrintWriter(socket2.getOutputStream(), true);
                out.println(msguniqueid);//send data to server
                socket2.close();

                Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT4));
                out = new PrintWriter(socket3.getOutputStream(), true);
                out.println(msguniqueid);//send data to server
                socket3.close();



            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }


    }
    static class  FIFO_Total {

        public  void  send_proposed_priority(String toclient, String proposed_priority) {

            try {


                Log.d(TAG,"toclient is "+toclient);

                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(toclient));
                //socket0.setSoTimeout(5000);
                PrintWriter out = new PrintWriter(socket0.getOutputStream(), true);

                //piggybacking message with unique id which is its port number in this case
                Log.d(TAG,"proposed priority is"+proposed_priority);
                out.println("pp,"+proposed_priority);//send proposed priority appended with "pp" to differentiate between message and priority

                socket0.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.e(TAG,"Socket timed out!"+e.getMessage());

                Log.e(TAG,"failed port is"+toclient);

            } catch(InterruptedIOException e){
                Log.e(TAG,"socket timed out!!");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

        }
        static int counter=0;
        public void send_message_with_agreedpriority(String message,Double agreedpriority){
            PrintWriter out;
            String msgwithagreedpriority = "ap,"+message+","+agreedpriority;
            try {


                Socket socket5 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT0));

                out = new PrintWriter(socket5.getOutputStream(), true);
                //piggybacking message with unique id which is its port number in this case

                out.println(msgwithagreedpriority);//send data to server

                socket5.close();
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.e(TAG,"Socket timed out!"+e.getMessage());
                //size=4;
                Log.e(TAG,"failed port is"+REMOTE_PORT0);

            } catch(InterruptedIOException e){
                Log.e(TAG,"socket timed out!!");
            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            try {
                counter++;
                Socket socket6 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT1));


                out = new PrintWriter(socket6.getOutputStream(), true);
                out.println(msgwithagreedpriority);//send data to server

                socket6.close();
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.e(TAG,"Socket timed out!"+e.getMessage());
                //size=4;
                Log.e(TAG,"failed port is"+REMOTE_PORT1);
            } catch(InterruptedIOException e){
                Log.e(TAG,"socket timed out!!");
            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
               try {
                   counter++;
                   Socket socket7 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(REMOTE_PORT2));
                //   socket7.setSoTimeout(5000);
                   out = new PrintWriter(socket7.getOutputStream(), true);
                   out.println(msgwithagreedpriority);//send data to server

                   socket7.close();
               }catch (UnknownHostException e) {
                   Log.e(TAG, "ClientTask UnknownHostException");
               } catch (SocketTimeoutException e) {
                   Log.e(TAG,"Socket timed out!"+e.getMessage());
                  // size=4;
                   Log.e(TAG,"failed port is"+REMOTE_PORT2);
               } catch(InterruptedIOException e){
                   Log.e(TAG,"socket timed out!!");
               }catch (IOException e) {
                   Log.e(TAG, "ClientTask socket IOException");
               }
            try {

                Socket socket8 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT3));
                //socket8.setSoTimeout(5000);
                out = new PrintWriter(socket8.getOutputStream(), true);
                out.println(msgwithagreedpriority);//send data to server

                socket8.close();
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.e(TAG,"Socket timed out!"+e.getMessage());
                //size=4;
                Log.e(TAG,"failed port is"+REMOTE_PORT3);
            } catch(InterruptedIOException e){
                Log.e(TAG,"socket timed out!!");
            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            try{

                Socket socket9 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT4));

                out = new PrintWriter(socket9.getOutputStream(), true);
                out.println(msgwithagreedpriority);//send data to server

                socket9.close();

                Log.d(TAG,message+" "+"has been sent "+counter);
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.e(TAG,"Socket timed out!"+e.getMessage());
                Log.e(TAG,"failed port is"+REMOTE_PORT4);
               // size=4;

            } catch(InterruptedIOException e){
                Log.e(TAG,"socket timed out!!");
            }catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
