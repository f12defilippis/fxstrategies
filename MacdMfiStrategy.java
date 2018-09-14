package jforex;

import com.dukascopy.api.*;
import java.util.*;

public class MacdMfiStrategy implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    
    @Configurable("selectedInstrument:")
    public Instrument selectedInstrument = Instrument.EURUSD;
    @Configurable("selectedPeriod:")
    public Period selectedPeriod = Period.FIVE_MINS;
    @Configurable("risk_profile:")
    public double risk_profile = 0.5;
    @Configurable("entryPips:")
    public int entryPips = 5;
    @Configurable("stopLossPips:")
    public int stopLossPips = 10;
    @Configurable("takeProfitPips:")
    public int takeProfitPips = 20;    
     
    int candlesToWait = 5;
   
    
    
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();

        context.setSubscribedInstruments(Collections.singleton(selectedInstrument), true);
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

        




        double [] todayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 1);
        double [] yesterdayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 2);
        
        double macd_today = todayMACD[0];
        double macd_signal_today = todayMACD[1];

        double macd_yesterday = yesterdayMACD[0];
        double macd_signal_yesterday = yesterdayMACD[1];

  

        //double mfi = indicators.mfi(selectedInstrument, selectedPeriod, OfferSide.BID, 14, 0);    // <10 >90    
        


        boolean goLong = false;
        boolean goShort = false;
        
    
        
        
        boolean macdBullishSignal = macd_yesterday < macd_signal_yesterday && macd_today > macd_signal_today && macd_today < 0;
        boolean macdBearishSignal = macd_yesterday > macd_signal_yesterday && macd_today < macd_signal_today && macd_today > 0;
        
        if(macdBullishSignal)
        {
             boolean bmfi = false;
             boolean brsi = false;
             for(int i = 0 ; i < 4 ; i++)
             {
                 double mfi = indicators.mfi(selectedInstrument, selectedPeriod, OfferSide.BID, 14, i);    // <10 >90    
                 if(mfi <= 15)
                 {
                     bmfi = true;
                 }
                 double rsi = indicators.rsi(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, i);  
                 if(rsi <= 30)
                 {
                     brsi = true;
                 }
             }
             goLong = bmfi && brsi;

        }
        
        if(macdBearishSignal)
        {
             boolean bmfi = false;
             boolean brsi = false;
             for(int i = 0 ; i < 4 ; i++)
             {
                 double mfi = indicators.mfi(selectedInstrument, selectedPeriod, OfferSide.BID, 14, i);    // <10 >90    
                 if(mfi >= 85)
                 {
                     goShort = true;
                     break;
                 }
                 double rsi = indicators.rsi(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, i);  
                 if(rsi <= 70)
                 {
                     brsi = true;
                 }
             }
             goShort = bmfi && brsi;
        }
         
        if(goLong)
        {
                for (IOrder order : engine.getOrders(selectedInstrument)) {
                    if (order.getState() == IOrder.State.OPENED && !order.isLong()) {
                        order.close();
                    }
                }            
                 
                 ITick lastTick = history.getLastTick(instrument);
                 double price = askBar.getClose();
                 double sl = price - instrument.getPipValue() * stopLossPips; 
                

                 double tp = price + instrument.getPipValue() * takeProfitPips;
                 
                 sl = round(sl, 5);

                 double quantity = getLot(price,sl);
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("BuyStopOrder", instrument, IEngine.OrderCommand.BUYSTOP,0.025, price, 0, sl, tp, gtt, "BUY");
                 
                  console.getOut().println("BuyStopOrder - Price: " + price + " SL: " + sl + " TP: " + tp);
            
        }else if(goShort)
        {
            
                for (IOrder order : engine.getOrders(selectedInstrument)) {
                    if (order.getState() == IOrder.State.OPENED && order.isLong()) {
                        order.close();
                    }
                }            
            
                 ITick lastTick = history.getLastTick(instrument);
                 double price = lastTick.getAsk();
                 double sl = 0;

                 if(stopLossPips > 0)
                 {
                     sl = price + instrument.getPipValue() * stopLossPips;
                 }
                 
                 double tp = price + (price - sl);
                 if(takeProfitPips > 0)
                 {
                     tp = price - instrument.getPipValue() * takeProfitPips;
                 }
  

                 double quantity = getLot(price,sl);
                 
                                                   
                 //double tp = price - instrument.getPipValue() * takeProfitPips;
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("SellStopOrder", instrument, IEngine.OrderCommand.SELLSTOP, 0.025, price, 20, sl, tp, gtt, "SELL"); 

                  console.getOut().println("SellStopOrder - Price: " + price + " SL: " + sl + " TP: " + tp);
        
        }
        
        
                      
               
        
    }
    
    private double getLot(double price, double stoploss) throws JFException
    {
        double lotSize = 0;

        double diff = price > stoploss ? price - stoploss : stoploss - price;

        double equity = context.getAccount().getEquity();
        
        double howCanIRiskPerTransaction = equity * risk_profile / 100d;


        lotSize = howCanIRiskPerTransaction / diff;

        lotSize /= 1000000d;                    // in millions
        return roundLot(lotSize);
    }

    private double roundLot(double lot)
    {   
        lot = (int)(lot * 1000) / (1000d);      // 1000 units min.
        return lot;
    }        
    
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
    
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
