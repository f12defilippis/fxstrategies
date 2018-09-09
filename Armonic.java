package jforex;

import com.dukascopy.api.*;
import java.text.DateFormat;
import java.util.*;
import java.text.SimpleDateFormat;

public class Armonic implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    
    @Configurable("selectedInstrument:")
    public Instrument selectedInstrument = Instrument.EURUSD;
    @Configurable("selectedPeriod:")
    public Period selectedPeriod = Period.TEN_MINS;
    @Configurable("stopLossPips:")
    public int stopLossPips = 25;
    @Configurable("takeProfitPips:")
    public int takeProfitPips = 50;

    @Configurable("minPeriod:")
    public int minPeriod = 2;
    @Configurable("maxPeriod:")
    public int maxPeriod = 100;



    private boolean lowCross = false;
    private boolean upperCross = false;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

        if (!instrument.equals(selectedInstrument) || !period.equals(selectedPeriod)) {
            return;
        }        
                
                                
        //0.5,0.618,0.382,0.999,0.886,0.999
        //abax_min,abax_max,cbab_min,cbab_max,adax_min,adax_max
        double abax_min = 0.618;
        double abax_max = 0.786;

        double cbab_min = 0.618;
        double cbab_max = 0.999;
        
        double adax_min = 0.786;
        double adax_max = 0.999;
        
        boolean d_found = false;
        
        //finding x point
        for(int x_index = minPeriod * 4 ; x_index <= maxPeriod ; x_index++)
        {
            if(d_found)
                break;

            IBar x_bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, x_index);


                        
            boolean isMin = isMin(x_index - minPeriod*2,x_index + minPeriod*2,x_bar);
//            console.getOut().println("Looking for X: " + DATE_FORMAT.format(x_bar.getTime()) + " Index: " + x_index
//            + "Min: " + isMin + " Val: " + x_bar.getLow());                                          
            
            // possibly X point founded
            if(isMin && bidBar.getLow() > x_bar.getLow())
            {
                
//                console.getOut().println("X Found: " + DATE_FORMAT.format(x_bar.getTime()) + " Ora " + DATE_FORMAT.format(new Date()));                                            
                
                
                for(int a_index = x_index - minPeriod ; a_index > minPeriod * 3 ; a_index--)
                {
                    if(d_found)
                        break;

                    IBar a_bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, a_index);


                    boolean isMax = isMax(a_index - minPeriod*2,a_index + minPeriod*2,a_bar);
                    boolean isImpulsive = isMax(x_index + minPeriod,a_index + minPeriod*4,a_bar);

//                    console.getOut().println("Looking for A: " + DATE_FORMAT.format(a_bar.getTime()) + " Index: " + a_index
//                    + "Max: " + isMax + " Val: " + a_bar.getHigh());                                         
                    
                    // possibly A point founded 
                    if(isMax && isImpulsive)
                    {

//                        console.getOut().println("A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                        
                        
                        for(int b_index = a_index - minPeriod ; b_index > minPeriod * 2 ; b_index--)
                        {
                            
                            if(d_found)
                                break;
                            
                            IBar b_bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, b_index);


                            
                            boolean b_isMin = isMin(b_index - minPeriod*2,b_index + minPeriod*2,b_bar);
                            
                            double b_threshold_max = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * abax_min);
                            double b_threshold_min = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * abax_max);
                            
                            boolean b_isInRange = b_bar.getLow() > b_threshold_min && b_bar.getLow() < b_threshold_max;

//                            console.getOut().println("Looking for B: " + DATE_FORMAT.format(b_bar.getTime())+ " Index: " + b_index + " Min: " + b_isMin +
//                            " TMax: " + b_threshold_max + " Tmin: " + b_threshold_min + " Val: " + b_bar.getLow());                                                                      
                                                        
                                                                                                                
                            // Possibly B point found
                            if(b_isMin && b_isInRange)
                            {
                                
//                                console.getOut().println("B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                
                                for(int c_index = b_index - minPeriod ; c_index >= minPeriod ; c_index--)
                                {
                                    IBar c_bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, c_index);
                                    boolean c_isMax = isMax(0,c_index + minPeriod*2,c_bar);
                                    
                                    double c_threshold_max = b_bar.getLow() + (a_bar.getHigh() - b_bar.getLow()) * cbab_max;
                                    double c_threshold_min = b_bar.getLow() + (a_bar.getHigh() - b_bar.getLow()) * cbab_min;
                                    
                                    boolean c_isInRange = c_bar.getHigh() > c_threshold_min && c_bar.getHigh() < c_threshold_max;

//                                    console.getOut().println("Looking for C: " + DATE_FORMAT.format(c_bar.getTime())+ " Index: " + c_index + " Max: " + c_isMax +
//                                    " TMax: " + c_threshold_max + " Tmin: " + c_threshold_min + " Val: " + c_bar.getLow());                                                                      
                                    
                                                                                                            
                                    // Possibly C point found
                                    if(c_isMax && c_isInRange)
                                    {
                                        
                                        //console.getOut().println("C Found: " + DATE_FORMAT.format(c_bar.getTime()) + "B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            

                                                                                
                                        
                                        double d_threshold_max = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * adax_min);
                                        double d_threshold_min = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * adax_max);

//                                        console.getOut().println("Looking for D: " + DATE_FORMAT.format(bidBar.getTime())+ " Index: 0 TMax: " + d_threshold_max + " Tmin: " + d_threshold_min + " Val: " + bidBar.getLow());                                                                      
                                        
                                        d_found = bidBar.getLow() > d_threshold_min && bidBar.getLow() < d_threshold_max;
                                        if(d_found)
                                        {
                                            d_found = d_found && isMin(a_index,x_index,x_bar);
                                            d_found = d_found && isMax(a_index,x_index,a_bar);
                                            d_found = d_found && isMin(b_index,a_index,b_bar);
                                            d_found = d_found && isMax(b_index,a_index,a_bar);
                                            d_found = d_found && isMin(c_index,b_index,b_bar);
                                            d_found = d_found && isMax(c_index,b_index,c_bar);
                                            d_found = d_found && isMax(0,c_index,c_bar);
                                            d_found = d_found && isMin(0,c_index,bidBar);
                                        }
                                        
                                        if(d_found)
                                        {
                                            console.getOut().println("PATTERN FOUND: " + DATE_FORMAT.format(bidBar.getTime()) + 
                                            "C Found: " + DATE_FORMAT.format(c_bar.getTime()) + "B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                            console.getOut().println("D: " + bidBar.getLow() + 
                                            " C: " + c_bar.getHigh() + " B: " + b_bar.getLow() + " A: " + a_bar.getHigh() + " X: " + x_bar.getLow());                                            

                                            
                                            
                                            double stopLoss = bidBar.getClose() - selectedInstrument.getPipValue() * stopLossPips;
                                            double takeProfit = bidBar.getClose() + selectedInstrument.getPipValue() * takeProfitPips;

                                            //double takeProfit = a_bar.getHigh() - (a_bar.getHigh() - bidBar.getLow()) * 0.618;
                                            //double stopLoss = a_bar.getHigh() - (a_bar.getHigh() - x_bar.getLow()) * 1.13;
                            
                                                        
                                                                                                                
                                            engine.submitOrder(getLabel(bidBar.getTime()), selectedInstrument, IEngine.OrderCommand.BUY, 0.1, 0, 5, stopLoss, takeProfit, 0, "");                                                                                       
                                                                                                                                                                                
                                            break;
                                        }
                                    }
                                    
                                }
                            }
                            
                        }
                    }
                    
                    
                }
            }
            
            
            
        }
        
        
        
        
        
    }
    
    private boolean isMin(int indexFrom, int indexTo, IBar barCheck) throws JFException 
    {
        boolean ret = true;
        for(int i = indexFrom ; i <= indexTo ; i++)
        {
            IBar prevBar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, i);
            if(prevBar.getLow() < barCheck.getLow())
                return false; 
        }
        return ret;
    }
    
    private boolean isMax(int indexFrom, int indexTo, IBar barCheck) throws JFException 
    {
        boolean ret = true;
        for(int i = indexFrom ; i <= indexTo ; i++)
        {
            IBar prevBar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, i);
            if(prevBar.getHigh() > barCheck.getHigh())
                return false; 
        }
        return ret;
    }

    private String getLabel(long time) {
        return "IVF" + DATE_FORMAT.format(time) + generateRandom(10000) + generateRandom(10000);
    }

    private String generateRandom(int n) {
        int randomNumber = (int) (Math.random() * n);
        String answer = "" + randomNumber;
        if (answer.length() > 3) {
            answer = answer.substring(0, 4);
        }
        return answer;
    }    
    
}
