package jforex;

import com.dukascopy.api.*;
import java.util.*;

public class FractalScalpingStrategy2 implements IStrategy {
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
    //@Configurable("equity:")
    public double equity = 5000;
    @Configurable("risk_profile:")
    public double risk_profile = 0.5;
    //@Configurable("leverage:")
    public double leverage = 30;
    @Configurable("entryPips:")
    public int entryPips = 5;
    @Configurable("stopLossPips:")
    public int stopLossPips = 0;
    @Configurable("takeProfitPips:")
    public int takeProfitPips = 10;    
    //@Configurable("candlesToWait:")
    public int candlesToWait = 1;    
    //@Configurable("rsilow_min:")
    public int rsilow_min = 25;    
    //@Configurable("rsilow_max:")
    public int rsilow_max = 40;    
    //@Configurable("rsihigh_max:")
    public int rsihigh_max = 75;    
    //@Configurable("rsihigh_min:")
    public int rsihigh_min = 60;    
    //@Configurable("short_bool:")
    public int short_bool = 1;    
    //@Configurable("long_bool:")
    public int long_bool = 1;       
    @Configurable("ema_condition:")
    public int ema_condition = 1;       
    @Configurable("fractal_condition:")
    public int fractal_condition = 1;       
    @Configurable("rsi_condition:")
    public int rsi_condition = 2;  
    @Configurable("macd_condition:")
    public int macd_condition = 1;       
    @Configurable("candle_condition:")
    public int candle_condition = 2;       
    @Configurable("price_condition:")
    public int price_condition = 1;       
    @Configurable("hammer_condition:")
    public int hammer_condition = 1;       
    @Configurable("hammer_perc:")
    public double hammer_perc = 0.8;       
    @Configurable("sar_condition:")
    public int sar_condition = 1;       
   
    
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

        long time = bidBar.getTime();
        
        double ema6 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 6, 0);   
        double ema12 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 0);   
        double ema34 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 34, 0);

        double ema6_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 6, 1);   
        double ema12_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 1);   
        double ema34_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 34, 1);

        double adx = indicators.adx(selectedInstrument, selectedPeriod, OfferSide.BID, 14, 1);


        double sar = indicators.sar(selectedInstrument, selectedPeriod, OfferSide.BID, 0.02, 0.2, 0);


        double [] todayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 0);


        double [] fractals = indicators.fractalLines(instrument, period, OfferSide.BID, 5, 0);
        double [] fractals_y = indicators.fractalLines(instrument, period, OfferSide.BID, 5, 1);
        
        double rsi = indicators.rsi(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, 0);        
        IBar bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 0);
        IBar bar_y = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 1);

        boolean goLong = long_bool == 1 && adx > 20;
        boolean goShort = short_bool == 1 && adx > 20;
        
        if(sar_condition == 1)
        {
            goLong = goLong && bar.getLow() > sar;
            goShort = goShort && bar.getHigh() < sar;
        }
        
        if(hammer_condition == 1)
        {
            //goLong = goLong && bar_y.getClose() >= bar_y.getLow() + (bar_y.getHigh()-bar_y.getLow()) * hammer_condition;
            //goShort = goShort && bar_y.getClose() <= bar_y.getLow() + (bar_y.getHigh()-bar_y.getLow()) * (1.0-hammer_condition);

            goLong = goLong && bar_y.getClose() == bar_y.getHigh();
            goShort = goShort && bar_y.getClose() == bar_y.getLow();

            //console.getOut().println("Prices - today: " + bar_y.getClose() + " High: " + bar_y.getHigh() + " Low: " + bar_y.getLow() + " Diff: " + (bar_y.getHigh()-bar_y.getLow()) + " Long: " + bar_y.getLow() + (bar_y.getHigh()-bar_y.getLow()) * (hammer_condition));
        
        }
        
        if(candle_condition > 0)
        {
            boolean possibleLong = true;
            boolean possibleShort = true;
            for(int i = 1 ; i <= candle_condition; i++)
            {
                IBar bar_i = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, i);
                if(bar_i.getOpen() > bar_i.getClose())
                {
                    possibleLong = false;
                }else
                {
                    possibleShort = false;
                }
            }
            goLong = goLong && possibleLong;
            goShort = goShort && possibleShort;
        }
        
        if(price_condition > 0)
        {
            boolean possibleLong = true;
            boolean possibleShort = true;
            for(int i = 0 ; i <= price_condition; i++)
            {
                IBar bar_yesterday = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, i+2);
                IBar bar_today = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, i+1);
                if(bar_yesterday.getClose() > bar_today.getClose())
                {
                    possibleLong = false;
                }else if(bar_yesterday.getClose() < bar_today.getClose())
                {
                    possibleShort = false;
                }
            }
            goLong = goLong && possibleLong;
            goShort = goShort && possibleShort;
        }
         
        
        if(macd_condition == 1)
        {
            goLong = goLong && todayMACD[0] > 0;
            goShort = goShort && todayMACD[0] < 0;
        }
        
        
        if(ema_condition == 1)
        {
            goLong = goLong && ema6 > ema12 && ema12 > ema34;
            goShort = goShort && ema6 < ema12 && ema12 < ema34;
        }else if(ema_condition == 2)
        {
            goLong = goLong && ema6 > ema12 && ema12 > ema34 && !(ema6_y > ema12_y && ema12_y > ema34_y);
            goShort = goShort && ema6 < ema12 && ema12 < ema34 && !(ema6_y < ema12_y && ema12_y < ema34_y);
        }
        
        if(fractal_condition == 1)
        {
            goLong = goLong && fractals[0] > ema6 && bar.getClose() > fractals[0] + instrument.getPipValue() * entryPips;
            goShort = goShort && fractals[1] < ema34 && bar.getClose() < fractals[1] - instrument.getPipValue() * entryPips;
        }else if(fractal_condition == 2)
        {
            goLong = goLong && fractals[0] > ema6 && bar.getClose() > fractals[0] + instrument.getPipValue() * entryPips && !(fractals_y[0] > ema6_y && bar_y.getClose() > fractals_y[0] + instrument.getPipValue() * entryPips);
            goShort = goShort && fractals[1] < ema34 && bar.getClose() < fractals[1] - instrument.getPipValue() * entryPips && !(fractals_y[1] < ema34_y && bar_y.getClose() < fractals_y[1] - instrument.getPipValue() * entryPips);
        }
            
        if(rsi_condition == 1)
        {
            goLong = goLong && rsi < rsihigh_max;
            goShort = goShort && rsi > rsilow_min;          
        }else if(rsi_condition == 2)
        {
           goLong = goLong && rsi < rsihigh_max && rsi > rsihigh_min;
           goShort = goShort && rsi > rsilow_min;          
        }
            
        if(goLong)
        {
                 ITick lastTick = history.getLastTick(instrument);
                 double price = lastTick.getAsk();
                 double sl = fractals[1];
                 
/*                 if(hammer_condition == 1)
                 {
                     sl = bar_y.getLow();
                 }
  */               
                 if(stopLossPips > 0)
                 {
                     sl = price - instrument.getPipValue() * stopLossPips;
                 }

                 double tp = price + (price - sl);
                 if(takeProfitPips > 0)
                 {
                     tp = price + instrument.getPipValue() * takeProfitPips;
                 }
                 
                 
                 double risk = price - sl;
                 double howMuchCanIRisk = equity * risk_profile / 100;
                 double singleMicroLotsRisk = risk * 1000 / leverage;                 
                 int howManyMicroLots = (int)(singleMicroLotsRisk / howMuchCanIRisk);
                 double quantity = (double)howManyMicroLots / 100.0;     
                 
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("BuyStopOrder", instrument, IEngine.OrderCommand.BUYSTOP, 0.02, price, 20, sl, tp, gtt, "BUY");
                 
                  console.getOut().println("BuyStopOrder - Price: " + price + " SL: " + sl + " TP: " + tp);
            
        }else if(goShort)
        {
                 ITick lastTick = history.getLastTick(instrument);
                 double price = lastTick.getAsk();
                 double sl = fractals[0];
    /*             if(hammer_condition == 1)
                 {
                     sl = bar_y.getHigh();
                 }
      */           
                 if(stopLossPips > 0)
                 {
                     sl = price + instrument.getPipValue() * stopLossPips;
                 }
                 
                 double tp = price + (price - sl);
                 if(takeProfitPips > 0)
                 {
                     tp = price - instrument.getPipValue() * takeProfitPips;
                 }
                 
                 
                 double risk = price - sl;
                 double howMuchCanIRisk = equity * risk_profile / 100;
                 double singleMicroLotsRisk = risk * 1000 / leverage;                 
                 int howManyMicroLots = (int)(singleMicroLotsRisk / howMuchCanIRisk);
                 double quantity = (double)howManyMicroLots / 100.0;     
                 
                 //double tp = price - instrument.getPipValue() * takeProfitPips;
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("SellStopOrder", instrument, IEngine.OrderCommand.SELLSTOP, 0.02, price, 20, sl, tp, gtt, "SELL"); 

                  console.getOut().println("SellStopOrder - Price: " + price + " SL: " + sl + " TP: " + tp);
        
        }
        
        
                      
               
        
    }
}