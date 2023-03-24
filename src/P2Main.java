import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class P2Main {
    
    static final int numOfCustomers = 50, numOfBOA = 2;
    Random r = new Random();
    static semaFinished[] finished;

    static Queue<String> queueBOA = new LinkedList<String>();
    static Queue<Integer> queueTT = new LinkedList<Integer>();
    static Queue<String> queueCSW = new LinkedList<String>();
    static ArrayList<Movie> movieList = new ArrayList<>();
    static Semaphore mutexBOAQueue = new Semaphore(1);
    static Semaphore mutexTTQueue = new Semaphore(1);
    static Semaphore mutexCSWQueue = new Semaphore(1);
    static Semaphore mutexMovie = new Semaphore(1);
    static Semaphore readyBOACustomer = new Semaphore(0);
    static Semaphore readyTTCustomer = new Semaphore(0);
    static Semaphore readyCSWCustomer = new Semaphore(0);

    public static class semaFinished{
        Semaphore served;
        boolean message;

        public semaFinished(Semaphore served, Boolean message){
            this.served = served;
            this.message = message;
        }
    }

    public static class Movie{
        private String Name;
        private int ID, seatsAvailable;

        Movie(String name, int id, int seatsavailable){
            this.Name = name;
            this.ID = id;
            this.seatsAvailable = seatsavailable;
        }

        public int getID() {
            return ID;
        }
        public String getName() {
            return Name;
        }
        public int getSeatsAvailable() {
            return seatsAvailable;
        }
        public void removeSeat(){
            this.seatsAvailable--;
        }
    }
    
    public class Customer extends Thread{
        private int ID, movie;
        private String customerOrder;

        public Customer(int id){
            this.ID = id;
            movie = r.nextInt(movieList.size());
        }
        
        @Override
        public void run(){
            try{
                System.out.println("Customer " + ID + " created, buying ticket to " + movieList.get(movie).getName());
                mutexBOAQueue.acquire();
                queueBOA.add(ID + " " + movie);
                readyBOACustomer.release();
                mutexBOAQueue.release();
                finished[ID].served.acquire();

                if(finished[ID].message){
                    mutexTTQueue.acquire();
                    queueTT.add(ID);
                    System.out.println("Customer " + ID + " in line to see ticket taker");
                    readyTTCustomer.release();
                    mutexTTQueue.release();
                    finished[ID].served.acquire();

                    if(visitCS()){
                        orderCST();
                        mutexCSWQueue.acquire();
                        queueCSW.add(ID + " " + customerOrder);
                        System.out.println("Customer " + ID + " in line to buy " + customerOrder);    
                        readyCSWCustomer.release();
                        mutexCSWQueue.release();
                        finished[ID].served.acquire();
                    }
                    else{
                        System.out.println("Customer " + ID + " did not go to the Concession Stand");

                    }
                    System.out.println("Customer " + ID + " enters theater to see " + movieList.get(movie).getName());
                }
                else{
                    System.out.println(movieList.get(movie).getName()+ " sold out, ticket not sold to customer " + ID);
                }
            }
            catch(InterruptedException e){
                System.err.println("Error with Customer Thread " + ID + "\nTerminating program...");
                System.exit(0);
            }

        }

        private void orderCST(){
            int order = r.nextInt(3);
            if(order == 0){
                this.customerOrder = "Popcorn";
            }
            else if(order == 1){
                this.customerOrder = "Soda";
            }
            else {
                this.customerOrder = "Popcorn & Soda";

            }
        }

        private boolean visitCS(){
            int num = r.nextInt(2);
            if(num == 0){
                return true;
            }
            else{
                return false;
            }
        }
    }
        
    public class BoxOfficeAgent extends Thread{
        private int ID, customerID, customerMovie;

        public BoxOfficeAgent(int id){
            this.ID = id;
            System.out.println("Box Office Agent " + ID + " created");
        }

        @Override
        public void run(){
            while (true) {
                try {
                    readyBOACustomer.acquire();
                    mutexBOAQueue.acquire();
                    removeBOA();
                    mutexBOAQueue.release();
                    mutexMovie.acquire();
                    movieAvailable();
                    mutexMovie.release();
                    beginBOAProcess();
                    finished[customerID].served.release();

                } catch (InterruptedException e) {
                    System.err.println("Error with BOA Thread " + ID + "\nTerminating Program...");
                    System.exit(0);
                }
            }

        }

        private void beginBOAProcess(){
            try{
                sleep(1500);
            }
            catch(InterruptedException e){}
            if(finished[customerID].message){
                System.out.println("Box office agent " + ID + " sold ticket for " + movieList.get(customerMovie).getName() + " to customer " + customerID);
            }
        }

        private void removeBOA(){
            String line = queueBOA.remove();
            String linearr[] = line.split(" ");
            customerID = Integer.parseInt(linearr[0]);
            customerMovie = Integer.parseInt(linearr[1]);
            System.out.println("Box office agent " + ID + " serving customer " + customerID);
        }

        private void movieAvailable(){
            if(movieList.get(customerMovie).getSeatsAvailable() > 0){
                movieList.get(customerMovie).removeSeat();
                finished[customerID].message = true;
            }
            else
                finished[customerID].message = false;
        }

    }
    public class TicketTaker extends Thread{
        private int customerID;
        
        public TicketTaker(){
            System.out.println("Ticket Taker created");
        }

        @Override
        public void run(){
            while(true){
                try {
                    readyTTCustomer.acquire();
                    mutexTTQueue.acquire();
                    removeTT();
                    mutexTTQueue.release();
                    beginProcess();
                    finished[customerID].served.release();
                } catch (InterruptedException e) {
                    System.err.println("Error in Ticker Tacker thread\nTerminating Program...");
                    System.exit(0);
                }
            }
        }

        private void beginProcess(){
            try {
                sleep(250);
            } catch (InterruptedException e) {}
            System.out.println("Ticket taken from customer " + customerID);
        }

        private void removeTT(){
            customerID = queueTT.remove();
        }
    }
    public class ConcessionStandWorker extends Thread{
    private int customerID;
    private String order;
        
        public ConcessionStandWorker(){
            System.out.println("Concession Stand Worker created");
        }

        @Override
        public void run(){
            while(true){
                try {
                    readyCSWCustomer.acquire();
                    mutexCSWQueue.acquire();
                    removeCSW();
                    mutexCSWQueue.release();
                    beginProcess();
                    finished[customerID].served.release();
                } catch (InterruptedException e) {
                    System.err.println("Error in Concession Stand worker thread\nTerminating Program...");
                    System.exit(0);
                }
                
                
            }
        }

        private void beginProcess(){
            System.out.println("Order for " + order + " taken from customer " + customerID);
            try {
                sleep(3000);
            }
            catch (InterruptedException e) {}
            System.out.println(order + " given to customer " + customerID);
        }

        private void removeCSW(){
            String line = queueCSW.remove();
            String linearr[] = line.split(" ");
            customerID = Integer.parseInt(linearr[0]);
            order = linearr[1];
        }
    }    
    public static void main(String[] args) throws FileNotFoundException {

        File movieInput = new File(args[0]);
        Scanner movieSCN = new Scanner(movieInput);
        int count = 0;

        while(movieSCN.hasNextLine()){

            String line = movieSCN.nextLine();
            String linearr[] = line.split("\t");
            Movie mv = new Movie(linearr[0], count+1, Integer.parseInt(linearr[1]));
            movieList.add(count, mv);
            count++;
        }

        P2Main pm = new P2Main();
        for(int i = 0; i < numOfBOA; i++){
            BoxOfficeAgent boa = pm.new BoxOfficeAgent(i);
            boa.start();
        }

        Thread[] cThread = new Thread[numOfCustomers];
        finished = new semaFinished[numOfCustomers];
        for(int i = 0; i < numOfCustomers; i++){
            cThread[i]=pm.new Customer(i);
            finished[i] = new semaFinished(new Semaphore(0), false);
            cThread[i].start();
        }

        TicketTaker tt = pm.new TicketTaker();
        tt.start();

        ConcessionStandWorker csw = pm.new ConcessionStandWorker();
        csw.start();

        for(int i = 0; i < numOfCustomers; i++){
            try {
                cThread[i].join();
                System.out.println("Joined Customer " + i);
            } catch (Exception e){}
        }
        movieSCN.close();
        System.exit(0);

        
    
    }
}