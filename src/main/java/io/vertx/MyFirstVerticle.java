package io.vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.buffer.Buffer;
import java.io.*;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.Comparator;

public class MyFirstVerticle extends AbstractVerticle {
    private final int InitialBal  = 10000 ;

    public static class TransactionDetails{
        public int item;
        public int qty;
    }

    public static class StockOfPos implements Serializable{
        public static int totItem;
        public static int col;
        private long transactionAmt;
        private static final int  MaxTransaction= 10 ;
        private int counter;
        private  TransactionDetails []sold ;
        private static long sellingItem[];
        private int posInventory[][] = {
                {10,50000},
                {10,25000},
                {5,60000},
                {25,30000},
                {12,50000},
                {4,20000},
                {30,50000}
        };
        private static String itemName [] = {
                "CUP OF MILK 200ML",
                "BISCUITS(200g)",
                "CUP OF TEA",
                "PACKED LUNCH",
                "MINERAL WATER BOTTLE",
                "BANANA (1Pc)",
                "LUNCH MEAL"
        };
        public StockOfPos()
        {
            totItem = posInventory.length;
            col = posInventory[0].length;
            sellingItem  = new long[totItem];

            for(int i=0;i<sellingItem.length;i++)
            {
                sellingItem[i] = 0;
            }
            sold = new TransactionDetails[MaxTransaction];
            counter = 0;
            transactionAmt  = 0;
        }
        public StockOfPos(int it,int cl , long []si,TransactionDetails []so,int ct,int trAmt)
        {
            totItem  = it;
            col  = cl;
            sellingItem = si;
            sold  = so;
            counter = ct;
            transactionAmt = trAmt;
        }

        public long totalTransaction()
        {
            return transactionAmt;
        }
        public TransactionDetails[] last10Transaction()
        {
            return sold;
        }
        public static  String[][] getTopSellingItem()
        {
            String result[][] = new String[totItem][2];

            for (int i = 0;i<totItem;i++)
            {
                result[i][0] = itemName[i];
                result[i][1]  = ""+sellingItem[i];
            }
            // Sort according to top selling item

            Arrays.sort(result, new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {

                      long n1 = Long.parseLong(o1[1]);
                      long n2  = Long.parseLong(o2[1]);
                    //System.out.println(n1+" "+n2);
                      return n1>=n2?-1:1;    // descending order
                }
            });
            return result;
        }
        public void purchase(int itemNo , int qty)
        {
            //System.out.println("in purchase "+itemNo+" qty "+qty);
            // update the last 10 transactions
            if(sold[counter] ==null)
            {
                sold[counter] = new TransactionDetails();
            }
            sold[counter].item = itemNo ;
            sold[counter].qty  =qty;
            counter  = (counter+1) % MaxTransaction;

            // detuct from inventory Stock
            posInventory[itemNo-1][1] -=qty;

            // update total transaction for POS

            transactionAmt += transactionDone(itemNo,qty);

            // upadate the selling item;

            sellingItem[itemNo-1] +=qty;

           // System.out.println("Success"+transactionAmt);

        }
        public long transactionDone(int itemNo , int qty)
        {
            return (qty*posInventory[itemNo-1][0]);
        }
        public int getStock(int itemNo){
            return posInventory[itemNo-1][1];
        }


    }

    public static class Card{

        public static HashMap<String,Integer> cardDetails;

        public Card(int user)
        {
        cardDetails  = new HashMap<>(user);
        }
        public static String issueCard(int bal, String cardNo)
        {
            cardDetails.put(cardNo,bal);
            return cardNo;

        }
        public static boolean isValidCard(String card)
        {
            return cardDetails.containsKey(card);
        }
        public static int getBalance(String cardNo)
        {
            if(!isValidCard(cardNo))
                return 0;
            return cardDetails.get(cardNo);
        }
        public static void updateBalance(long amt,String card)
        {

            int balance = cardDetails.get(card);

            cardDetails.put(card,(int)(balance-amt));
        }
        public static void refillCard(long amt , String card)
        {
            int balance;
            if(!isValidCard(card))
                balance  = 0;
            else
                balance = cardDetails.get(card);

            cardDetails.put(card,(int)(balance+amt));
        }

    }

    final String pos,issue,user,noOfTrans;
    StockOfPos database[];
    Card cardIssued;
    public final int base = 6001; // inially set base address for port no
    public ArrayList<Integer> listOfPos , listOfKiosk ;
    public ArrayList<String> cardId;


    public MyFirstVerticle(String pos,String issue , String user ,String not)
    {
       this.pos = pos;
       this.issue = issue;
       this.user  = user;
       this.noOfTrans = not;
       database  = new StockOfPos[Integer.parseInt(pos)];
       for (int i = 0;i<database.length;i++)
       {
           database[i] = new StockOfPos();
       }
       cardIssued  = new Card(Integer.parseInt(user));
       listOfKiosk = new ArrayList<>(Integer.parseInt(this.issue));
       listOfPos = new ArrayList<>(Integer.parseInt(this.pos));
       cardId = new ArrayList<>(Integer.parseInt(this.user));

       // First N  port is assigned to POS
       for(int i = 0;i<Integer.parseInt(this.pos);i++)
       {
           listOfPos.add((base+i));
       }
       // After POS , Kiosk Port is Assigned
       for (int i = 0;i<Integer.parseInt(this.issue);i++)
       {
           listOfKiosk.add((base+i+listOfPos.size()));
       }
       for (int i = 0;i<Integer.parseInt(this.user);i++)
       {
           cardId.add("csl"+i);
       }
      // System.out.println(listOfKiosk.size()+" KIOSK"+listOfPos.size()+" POS"+cardId.size()+" USER");
       //System.out.println(issue+" "+pos+" "+user);
    }

    @Override
    public void start(Future<Void> fut) {

        int portNo = 7778;
        System.out.println("Simulation Started at Port no :"+portNo);
        // POS Terminal
        for(int i = 0;i<listOfPos.size();i++)
        {
            System.out.println("POS up @Port No "+listOfPos.get(i));
            createPosTerminal(listOfPos.get(i));
        }

        // Create KIOSK teminal
        for(int i = 0;i<listOfKiosk.size();i++)
        {
            System.out.println("Kiosk up @Port No "+listOfKiosk.get(i));
            createKioskTerminal(listOfKiosk.get(i));
        }
        //TODO : issue card to each person
        for (int i =0;i<cardId.size();i++)
        {
            // Assigning Random Kiosk For Card Issue
            int rd  = (int)(Math.random()*100)%listOfKiosk.size();
            int port = listOfKiosk.get(rd);
            String query="/?bal="+this.InitialBal+"&cardId="+cardId.get(i);
            System.out.println("Sending Query @ Kiosk port "+port+ "Query given"+query);
            createClient(port,query);
        }
        vertx
                .createHttpServer()
                .requestHandler((HttpServerRequest r) -> {


                    // TODO :Run Simulation
                    // Note Addressing noOfTrans queries per Refresh  as per given in input

                    int i=Integer.parseInt(this.noOfTrans);
                    while (i--!=0)
                    {
                        int rd  = (int)(Math.random()*100);
                        int no = (rd % StockOfPos.totItem ) + 1;
                        int card  =(rd % cardId.size());
                        int tt = (rd %100)+1;
                        String query = "/?qty="+tt+"&itemId="+no+"&cardId="+cardId.get(card);
                        createClient(base+((int)(Math.random()*100)%Integer.parseInt(this.pos)), query);
                    }

                    System.out.println("\nCard issued and Balance Remaining\n");
                    for (String k:cardId) {
                        System.out.println("Card Id :"+k+" Balance "+Card.getBalance(k));
                    }


                    //TODO : Show in table Format
                   /* String topSelling = " <div> <table><tr> <th>Item Name </th> <th>Quantity </th> \n<br>";
                    String posTransaction = "<div> <table><tr> <th>POS no</th> <th> Transaction Amt(Rs) </th> \n<br>";

                    // Get top selling item from database
                    String okk[][] = StockOfPos.getTopSellingItem();
                    for (i = 0; i < okk.length; i++) {
                        topSelling += "<th>"+okk[i][0]+"</th> <th>"+okk[i][1]+"</th> \n<br>";
                    }
                    // Get Transaction amount of ach POS from Database

                    for (i =0;i<database.length;i++)
                    {
                        posTransaction +="<th>"+(i+1)+"</th> <th>"+database[i].transactionAmt+"</th> \n<br>";
                    }

                    topSelling += "  </tr></table> </div> \n";

                    posTransaction += " </tr></table></div>\n";

                    */

                   String topSelling = "Item Name --|-- Quantity\n<br>";
                   String posTransaction = "POS no --|-- Transact Amt(Rs.)\n<br>";
                    // Get top selling item from database
                    String okk[][] = StockOfPos.getTopSellingItem();
                    for (i = 0; i < okk.length; i++) {
                        topSelling += ""+okk[i][0]+"--|--"+okk[i][1]+"\n<br>";
                    }
                    // Get Transaction amount of ach POS from Database

                    for (i =0;i<database.length;i++)
                    {
                        posTransaction +=""+(i+1)+"--|--"+database[i].transactionAmt+"\n<br>";
                    }

                    r.response().end(
                            "<!DOCTYPE html>\n" +
                                    "<html>    \n" +
                                    "<head><title>Simulation</title></head>\n" +
                                    "<body>"+

                                    " <h2> Note : If on refreshing , List does not get updated , then Its time to refill the card. <br>" +
                                            "Refer Readme For Syntax ,See Terminal For Card balance  </h2> <br>"+
                                    "  <fieldset>  <form id='simulation' action='http://localhost:7778/?refresh=1' method='post'>\n" +
                                    "    <div class=\"button\">\n" +
                                    "        <button type=\"submit\"> Refresh List </button>\n" +
                                    "    </div>" +
                                    "    </form>         \n" +
                                    "<h1> Top Selling item </h1><br>"+topSelling+" "+
                                    "<h1> POS Transaction Details </h1><br>"+posTransaction+" </fieldset>" +
                                    "" +
                                    "</body>    \n" +
                                    "</html>");
                })
                .listen(portNo, result -> {
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });


    }

    public void createKioskTerminal(int portNo) {
        //TODO add kiosk
        vertx
                .createHttpServer().requestHandler((HttpServerRequest r) -> {
            String bal = r.getParam("bal");
            String cardId = r.getParam("cardId");

            if(bal==null || cardId == null)
            {
                r.response().end("<h1>Please provide Parameters \"bal\" and \"cardId\"</h1>");
            }
            else
            {
                if(!Card.isValidCard(cardId)) {
                    Card.issueCard(Integer.parseInt(bal), cardId);
                    System.out.println("Card Issued with id :" + cardId);
                    r.response().end("<h1>Card Issued with id :" + cardId + "</h1>");
                }
                else
                {
                    Card.refillCard(Long.parseLong(bal),cardId);
                    System.out.println("Refilled the card successfully , with amount :Rs "+bal);
                    int balance = Card.getBalance(cardId);
                    System.out.println("Balance on card ID :"+cardId+" is :Rs"+balance);
                    r.response().end("<h1>Refilled the Card Successfully <br> Balance now on CARD ID : "+cardId+"IS : Rs"+balance+"</h1>");
                }
            }

        })
                .listen(portNo, result -> {
                    if (result.succeeded()) {
                        System.out.println("Kiosk Started");
                    } else {
                        System.out.println("Kiosk Start Failed");
                    }
                });

    }

    public void createPosTerminal(int portNo)
    {
                HttpServer server  = vertx.createHttpServer();
                        server.requestHandler((HttpServerRequest r) -> {
                            int ap  = server.actualPort();
                          //  System.out.println("Request is at Port no :"+ap);
                    String tiid  = r.getParam("itemId");
                    String tqty  = r.getParam("qty");
                    String cardId = r.getParam("cardId");

                    if(tiid==null || tqty == null || cardId == null)
                    {
                        r.response().end("<h1>Please provide Query Parameters as \"qty\" \"itemId\" \"cardId\"</h1>");
                    }
                    else {
                        if (Card.isValidCard(cardId)) {
                            int iid, qty;
                            iid = qty = 0;
                            if (tqty != null) {
                                iid = Integer.parseInt((tiid));
                                qty = Integer.parseInt(tqty);
                            }
                            StockOfPos temp = database[ap - base];
                            if (qty != 0) {
                                if (qty < temp.getStock(iid)) {

                                    if (Card.getBalance(cardId) >= temp.transactionDone(iid, qty)) {
                                        //Deduct from invenory and update card balance
                                        temp.purchase(iid, qty);
                                        Card.updateBalance(temp.transactionDone(iid, qty), cardId);
                                        System.out.println("\n-----------------------------\nTransaction Details:\n" +
                                                "@POS NO : " + (ap - base + 1) + "\n" +
                                                "Item No Purchased:" + iid + "\n" +
                                                "Card ID used :"+cardId+" \n"+
                                                "Quantity :" + qty + "\n-------------------------------");

                                        r.response().end("Transaction Amount  =" + temp.transactionDone(iid, qty));
                                    } else {
                                        System.out.println("Insufficient fund , Please refill your card with ID "+cardId);
                                        System.out.println("Tranaction Amount = " + (temp.transactionDone(iid, qty)));
                                        System.out.println("Card Balance is :" + Card.getBalance(cardId));
                                        r.response().end("<h1> Insufficient fund , Please refill your card </h1>");
                                    }
                              /*  String ok[][] = temp.getTopSellingItem();
                                for (int i = 0; i < ok.length; i++) {
                                    System.out.println(ok[i][0] + "<=name :: sold qty => " + ok[i][1]);
                                }
                                int okk[][] = temp.posInventory;
                                for (int i = 0; i < okk.length; i++) {
                                    System.out.println(okk[i][0] + " \t " + okk[i][1]);
                                }
                                */


                                    String jso = "";
                        /*
                        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                        try {
                             jso = ow.writeValueAsString(temp);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }*/

                                    //String jso = Json.encodePrettily(temp);

                                    // System.out.println("Formmatted string\n" + jso);
                                    //   writeToFile("inp.txt",temp);


                                } else {
                                    System.out.println("Quantity demanded exceed the stock size");
                                    r.response().end("<h1>Quantity demanded exceed the stock size</h1>");
                                }
                            } else {
                                r.response().end("<h1>Please provide quantity of item</h1>");
                            }
                        } else {
                            r.response().end("<h1>Invalid card</h1>");
                        }
                    }
                })
                        .listen(portNo, result -> {
                    if (result.succeeded()) {
                        System.out.println("POS Started");
                    } else {
                        System.out.println("POS Start Failed");
                    }
                });

    }
    public void createClient(int portNo,String query)
    {
        // Send a GET request
        WebClient clients = WebClient.create(vertx);
        clients
                .get(portNo, "localhost",query)
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();
                       // System.out.println("Received response with status code" +  response.body().toString());
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage()+" Query given "+query+" Port No"+portNo);
                    }
                });

    }
}
