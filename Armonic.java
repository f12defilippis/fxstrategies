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
    
//    @Configurable("selectedInstrument:")
//    public Instrument selectedInstrument = Instrument.EURUSD;
    @Configurable("selectedPeriod:")
    public Period selectedPeriod = Period.TEN_MINS;
    //@Configurable("stopLossPips:")
    public int stopLossPips = 25;
    //@Configurable("takeProfitPips:")
    public int takeProfitPips = 50;

    @Configurable("minPeriod:")
    public int minPeriod = 4;
    @Configurable("maxPeriod:")
    public int maxPeriod = 100;

    @Configurable("risk_profile:")
    public double risk_profile = 1d;

    @Configurable("rsi_condition:")
    public int rsi_condition = 1;


    private boolean lowCross = false;
    private boolean upperCross = false;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    double equity = 0.0;    
    double howCanIRiskPerTransaction = 0.0;    
    double leverage = 0.0;    
    
    private double[] threshold_min = new double[Instrument.values().length];
    private double[] threshold_max = new double[Instrument.values().length];
    
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        
        
        equity = context.getAccount().getBaseEquity();
        
        howCanIRiskPerTransaction = equity * risk_profile / 100d;
        
        leverage = context.getAccount().getLeverage();
        
        console.getOut().println("Started - Equity: " + equity + " Lev: " + leverage + " Risk: " + howCanIRiskPerTransaction);
        
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

        if (!period.equals(selectedPeriod)) {
            return;
        }        

        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED) {
                double min = bidBar.getClose() < askBar.getClose() ? bidBar.getClose() : askBar.getClose();
                double max = bidBar.getClose() > askBar.getClose() ? bidBar.getClose() : askBar.getClose();
                if(min < threshold_min[instrument.ordinal()] && order.isLong())
                {
                    order.close();
                    console.getOut().println("Closing Order Long at price " + min);
                }
                if(max > threshold_max[instrument.ordinal()] && !order.isLong())
                {
                    order.close();
                    console.getOut().println("Closing Order Short at price " + max);
                }
            }
        }            
                
        double rsi = indicators.rsi(instrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, 0);        
                                                 
                                
        //0.5,0.618,0.382,0.999,0.886,0.999
        //abax_min,abax_max,cbab_min,cbab_max,adax_min,adax_max
        double abax_min = 0.618;
        double abax_max = 0.786;

        double cbab_min = 0.618;
        double cbab_max = 0.999;
        
        double adax_min = 0.786;
        double adax_max = 0.999;
        
        // Gartley Bullish
        checkBullish(instrument, period, askBar, bidBar, abax_min, abax_max, cbab_min, cbab_max, adax_min, adax_max, "GartleyBuy_" + instrument.getName().replace("/",""),rsi);   
        
        // Bat Bullish
        checkBullish(instrument, period, askBar, bidBar, 0.5, 0.618, 0.382, 0.999, 0.886, 0.999, "BatBuy_" + instrument.getName().replace("/",""),rsi);   

        // Gartley Bullish
        checkBearish(instrument, period, askBar, bidBar, abax_min, abax_max, cbab_min, cbab_max, adax_min, adax_max, "GartleySell_" + instrument.getName().replace("/",""),rsi);   
        
        // Bat Bullish
        checkBearish(instrument, period, askBar, bidBar, 0.5, 0.618, 0.382, 0.999, 0.886, 0.999, "BatSell_" + instrument.getName().replace("/",""),rsi);   
//        checkBearish(instrument, period, askBar, bidBar, 0.1, 0.618, 0.182, 0.999, 0.186, 0.999, "BatSell");   
       
              
                            
        
    }
    
    
    private void checkBullish(Instrument instrument, Period period, IBar askBar, IBar bidBar, double abax_min, double abax_max, double cbab_min, double cbab_max, double adax_min, double adax_max, String patternName, double rsi) throws JFException 
    {

      boolean d_found = false;
        
        //finding x point
        for(int x_index = minPeriod * 4 ; x_index <= maxPeriod ; x_index++)
        {
            if(d_found)
                break;

            IBar x_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, x_index);


                        
            boolean isMin = isMin(instrument,x_index - minPeriod*2,x_index + minPeriod*2,x_bar);
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

                    IBar a_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, a_index);


                    boolean isMax = isMax(instrument,a_index - minPeriod*2,a_index + minPeriod*2,a_bar);
                    boolean isImpulsive = isMax(instrument,x_index + minPeriod,a_index + minPeriod*4,a_bar);

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
                            
                            IBar b_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, b_index);


                            
                            boolean b_isMin = isMin(instrument,b_index - minPeriod*2,b_index + minPeriod*2,b_bar);
                            
                            double b_threshold_max = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * abax_min);
                            double b_threshold_min = a_bar.getHigh() - ((a_bar.getHigh() - x_bar.getLow()) * abax_max);
                            
                            boolean b_isInRange = b_bar.getLow() > b_threshold_min && b_bar.getLow() < b_threshold_max;

//                            console.getOut().println("Looking for B: " + DATE_FORMAT.format(b_bar.getTime())+ " Index: " + b_index + " Min: " + b_isMin +
//                            " TMax: " + b_threshold_max + " Tmin: " + b_threshold_min + " Val: " + b_bar.getLow());                                                                      
                                                        
                                                                                                                
                            // Possibly B point found
                            if(b_isMin && b_isInRange)
                            {
                                
 //                               console.getOut().println("B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                
                                for(int c_index = b_index - minPeriod ; c_index >= minPeriod ; c_index--)
                                {
                                    IBar c_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, c_index);
                                    boolean c_isMax = isMax(instrument,0,c_index + minPeriod*2,c_bar);
                                    
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
                                            d_found = d_found && isMin(instrument,a_index,x_index,x_bar);
                                            d_found = d_found && isMax(instrument,a_index,x_index,a_bar);
                                            d_found = d_found && isMin(instrument,b_index,a_index,b_bar);
                                            d_found = d_found && isMax(instrument,b_index,a_index,a_bar);
                                            d_found = d_found && isMin(instrument,c_index,b_index,b_bar);
                                            d_found = d_found && isMax(instrument,c_index,b_index,c_bar);
                                            d_found = d_found && isMax(instrument,0,c_index,c_bar);
                                            d_found = d_found && isMin(instrument,0,c_index,bidBar);
                                        }
                                        
                                        if(d_found && (rsi_condition == 1 ? rsi < 30 : true))
                                        {
                                           // console.getOut().println(patternName + "PATTERN FOUND: " + DATE_FORMAT.format(bidBar.getTime()) + 
                                            //"C Found: " + DATE_FORMAT.format(c_bar.getTime()) + "B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                            //console.getOut().println("D: " + bidBar.getLow() + 
                                            //" C: " + c_bar.getHigh() + " B: " + b_bar.getLow() + " A: " + a_bar.getHigh() + " X: " + x_bar.getLow());                                            

                                            for (IOrder order : engine.getOrders(instrument)) {
                                                if (order.getState() == IOrder.State.OPENED || order.getState() == IOrder.State.FILLED) {
                                                    return;
                                                }
                                            }                                              
                                            
                                            //double stopLoss = bidBar.getClose() - selectedInstrument.getPipValue() * stopLossPips;
                                            //double takeProfit = bidBar.getClose() + selectedInstrument.getPipValue() * takeProfitPips;

                                            int tpFactor = (int)(((a_bar.getHigh() - bidBar.getLow()) * 0.618) / instrument.getPipValue());
                                            int slFactor = (int)(((a_bar.getHigh() - bidBar.getLow()) * 1.13) / instrument.getPipValue());
                                            int entryFactor = (int)(((a_bar.getHigh() - x_bar.getLow()) * adax_min) / instrument.getPipValue());

                                            double takeProfit = a_bar.getHigh() - instrument.getPipValue() * tpFactor;
                                            double stopLoss = a_bar.getHigh() - instrument.getPipValue() * slFactor;
                                            double entryPrice = a_bar.getHigh() - instrument.getPipValue() * entryFactor;
                                            
                                             double piprisk = (entryPrice-stopLoss) / instrument.getPipValue();
                                            double stdLotPipValue = 10;
                                            
 //                                           double quantity = (double)(Math.round(howCanIRiskPerTransaction / (piprisk * stdLotPipValue) * 100000d)) / 100000d;
                                            double quantity = getLot(entryPrice,stopLoss);
                                                                                  
                                                                                                                
                                            engine.submitOrder(patternName, instrument, IEngine.OrderCommand.BUYSTOP, quantity, entryPrice, 0, stopLoss, takeProfit, 0, "");                                                                                       
                                            
                                            console.getOut().println("Order Submitted - " + patternName + " Qty: " + quantity + " Piprisk: " + piprisk + " PRICE: " + bidBar.getClose() + " EP: " + entryPrice + " SL: " + stopLoss + " TP: " + takeProfit + " Exit: " + d_threshold_min);
                                                                                                                                                                                                
                                            threshold_min[instrument.ordinal()] = d_threshold_min;                                                                                                                                                                                                                                                                                                        
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
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
    
    
    
    
    private void checkBearish(Instrument instrument, Period period, IBar askBar, IBar bidBar, double abax_min, double abax_max, double cbab_min, double cbab_max, double adax_min, double adax_max, String patternName, double rsi) throws JFException 
    {

      boolean d_found = false;
        
        //finding x point
        for(int x_index = minPeriod * 4 ; x_index <= maxPeriod ; x_index++)
        {
            if(d_found)
                break;

            IBar x_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, x_index);


                        
            boolean isMax = isMax(instrument,x_index - minPeriod*2,x_index + minPeriod*2,x_bar);
//            console.getOut().println("Looking for X: " + DATE_FORMAT.format(x_bar.getTime()) + " Index: " + x_index
//            + "Min: " + isMin + " Val: " + x_bar.getLow());                                          
            
            // possibly X point founded
            if(isMax && bidBar.getHigh() < x_bar.getHigh())
            {
                
                //console.getOut().println("X Found: " + DATE_FORMAT.format(x_bar.getTime()) + " Ora " + DATE_FORMAT.format(new Date()));                                            
                
                
                for(int a_index = x_index - minPeriod ; a_index > minPeriod * 3 ; a_index--)
                {
                    if(d_found)
                        break;

                    IBar a_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, a_index);


                    boolean isMin = isMin(instrument,a_index - minPeriod*2,a_index + minPeriod*2,a_bar);
                    boolean isImpulsive = isMin(instrument,x_index + minPeriod,a_index + minPeriod*4,a_bar);

//                    console.getOut().println("Looking for A: " + DATE_FORMAT.format(a_bar.getTime()) + " Index: " + a_index
//                    + "Max: " + isMax + " Val: " + a_bar.getHigh());                                         
                    
                    // possibly A point founded 
                    if(isMin && isImpulsive)
                    {

                       // console.getOut().println("A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                        
                        
                        for(int b_index = a_index - minPeriod ; b_index > minPeriod * 2 ; b_index--)
                        {
                            
                            if(d_found)
                                break;
                            
                            IBar b_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, b_index);


                            
                            boolean b_isMax = isMax(instrument,b_index - minPeriod*2,b_index + minPeriod*2,b_bar);
                            
                            double b_threshold_max = a_bar.getLow() + ((a_bar.getLow() - x_bar.getHigh()) * -abax_max);
                            double b_threshold_min = a_bar.getLow() + ((a_bar.getLow() - x_bar.getHigh()) * -abax_min);
                            
                            boolean b_isInRange = b_bar.getHigh() > b_threshold_min && b_bar.getHigh() < b_threshold_max;

//                            console.getOut().println("Looking for B: " + DATE_FORMAT.format(b_bar.getTime())+ " Index: " + b_index + " Min: " + b_isMin +
//                            " TMax: " + b_threshold_max + " Tmin: " + b_threshold_min + " Val: " + b_bar.getLow());                                                                      
                                                        
                                                                                                                
                            // Possibly B point found
                            if(b_isMax && b_isInRange)
                            {
                                
                                //console.getOut().println("B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                
                                for(int c_index = b_index - minPeriod ; c_index >= minPeriod ; c_index--)
                                {
                                    IBar c_bar = history.getBar(instrument, selectedPeriod, OfferSide.BID, c_index);
                                    boolean c_isMin = isMin(instrument,0,c_index + minPeriod*2,c_bar);
                                    
                                    double c_threshold_max = b_bar.getHigh() - (a_bar.getLow() - b_bar.getHigh()) * -cbab_min;
                                    double c_threshold_min = b_bar.getHigh() - (a_bar.getLow() - b_bar.getHigh()) * -cbab_max;
                                    
                                    boolean c_isInRange = c_bar.getLow() > c_threshold_min && c_bar.getLow() < c_threshold_max;

//                                    console.getOut().println("Looking for C: " + DATE_FORMAT.format(c_bar.getTime())+ " Index: " + c_index + " Max: " + c_isMax +
//                                    " TMax: " + c_threshold_max + " Tmin: " + c_threshold_min + " Val: " + c_bar.getLow());                                                                      
                                    
                                                                                                            
                                    // Possibly C point found
                                    if(c_isMin && c_isInRange)
                                    {
                                        
                                        //console.getOut().println("C Found: " + DATE_FORMAT.format(c_bar.getTime()) + "B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            

                                                                                
                                        
                                        double d_threshold_max = a_bar.getLow() + ((a_bar.getLow() - x_bar.getHigh()) * -adax_max);
                                        double d_threshold_min = a_bar.getLow() + ((a_bar.getLow() - x_bar.getHigh()) * -adax_min);

//                                        console.getOut().println("Looking for D: " + DATE_FORMAT.format(bidBar.getTime())+ " Index: 0 TMax: " + d_threshold_max + " Tmin: " + d_threshold_min + " Val: " + bidBar.getLow());                                                                      
                                        
                                        d_found = bidBar.getHigh() > d_threshold_min && bidBar.getHigh() < d_threshold_max;
                                        if(d_found)
                                        {
                                            d_found = d_found && isMax(instrument,a_index,x_index,x_bar);
                                            d_found = d_found && isMin(instrument,a_index,x_index,a_bar);
                                            d_found = d_found && isMax(instrument,b_index,a_index,b_bar);
                                            d_found = d_found && isMin(instrument,b_index,a_index,a_bar);
                                            d_found = d_found && isMax(instrument,c_index,b_index,b_bar);
                                            d_found = d_found && isMin(instrument,c_index,b_index,c_bar);
                                            d_found = d_found && isMin(instrument,0,c_index,c_bar);
                                            d_found = d_found && isMax(instrument,0,c_index,bidBar);
                                        }
                                        
                                        if(d_found && (rsi_condition == 1 ? rsi > 70 : true))
                                        {
                                          //  console.getOut().println(patternName + "PATTERN FOUND: " + DATE_FORMAT.format(bidBar.getTime()) + 
                                          //  "C Found: " + DATE_FORMAT.format(c_bar.getTime()) + "B Found: " + DATE_FORMAT.format(b_bar.getTime()) + " A Found: " + DATE_FORMAT.format(a_bar.getTime()) + " X Found: " + DATE_FORMAT.format(x_bar.getTime()));                                            
                                          //  console.getOut().println("D: " + bidBar.getHigh() + 
                                          //  " C: " + c_bar.getLow() + " B: " + b_bar.getHigh() + " A: " + a_bar.getLow() + " X: " + x_bar.getHigh());                                            

                                            for (IOrder order : engine.getOrders(instrument)) {
                                                if (order.getState() == IOrder.State.OPENED || order.getState() == IOrder.State.FILLED) {
                                                    return;
                                                }
                                            }                                              
                                            
                                            //double stopLoss = bidBar.getClose() - selectedInstrument.getPipValue() * stopLossPips;
                                            //double takeProfit = bidBar.getClose() + selectedInstrument.getPipValue() * takeProfitPips;

                                            int tpFactor = (int)(((bidBar.getHigh()-a_bar.getLow()) * 0.618) / instrument.getPipValue());
                                            int slFactor = (int)(((x_bar.getHigh()-a_bar.getLow()) * 1.13) / instrument.getPipValue());
                                            int entryFactor = (int)(((x_bar.getHigh()-a_bar.getLow()) * adax_min) / instrument.getPipValue());

                                            double takeProfit = a_bar.getLow() + instrument.getPipValue() * tpFactor;
                                            double stopLoss = a_bar.getLow() + instrument.getPipValue() * slFactor;
                                            double entryPrice = a_bar.getLow() + instrument.getPipValue() * entryFactor;

                                            double piprisk = (stopLoss - entryPrice) / instrument.getPipValue();
                                            double stdLotPipValue = 10d;
                                            
//                                            double quantity = (double)(Math.round(howCanIRiskPerTransaction / (piprisk * stdLotPipValue) * 100000d)) / 100000d;
                                            double quantity = getLot(entryPrice,stopLoss);
                                                                                    
                                                                                                                
                                            engine.submitOrder(patternName, instrument, IEngine.OrderCommand.SELLSTOP, quantity, entryPrice, 0, stopLoss, takeProfit, 0, "");                                                                                       
                                            
                                            console.getOut().println("Order Submitted - " + patternName + " Qty: " + quantity + " PipRisk: " + piprisk + " PRICE: " + bidBar.getHigh() + " EP: " + entryPrice + " SL: " + stopLoss + " TP: " + takeProfit + " Exit: " + d_threshold_max);
                                                                                                                                                                                                
                                            threshold_max[instrument.ordinal()] = d_threshold_max;                                                                                                                                                                                                                                                                                                        
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
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
    
    private double getLot(double price, double stoploss) throws JFException
    {
        double lotSize = 0;

        double diff = price > stoploss ? price - stoploss : stoploss - price;



        lotSize = howCanIRiskPerTransaction / diff;

        lotSize /= 1000000d;                    // in millions
        return roundLot(lotSize);
    }

    private double roundLot(double lot)
    {   
        lot = (int)(lot * 1000) / (1000d);      // 1000 units min.
        return lot;
    }    
    
    
    private boolean isMin(Instrument instrument, int indexFrom, int indexTo, IBar barCheck) throws JFException 
    {
        boolean ret = true;
        for(int i = indexFrom ; i <= indexTo ; i++)
        {
            IBar prevBar = history.getBar(instrument, selectedPeriod, OfferSide.BID, i);
            if(prevBar.getLow() < barCheck.getLow())
                return false; 
        }
        return ret;
    }
    
    private boolean isMax(Instrument instrument, int indexFrom, int indexTo, IBar barCheck) throws JFException 
    {
        boolean ret = true;
        for(int i = indexFrom ; i <= indexTo ; i++)
        {
            IBar prevBar = history.getBar(instrument, selectedPeriod, OfferSide.BID, i);
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
