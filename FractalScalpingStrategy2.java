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
    public double risk_profile = 1.0;
    //@Configurable("leverage:")
    public double leverage = 30;
    @Configurable("entryPips:")
    public int entryPips = 5;
    @Configurable("stopLossPips:")
    public int stopLossPips = 0;
    @Configurable("takeProfitPips:")
    public int takeProfitPips = 20;    
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
    public int rsi_condition = 0;  
    @Configurable("macd_condition:")
    public int macd_condition = 0;       
    @Configurable("candle_condition:")
    public int candle_condition = 1;       
    @Configurable("price_condition:")
    public int price_condition = 0;       
    @Configurable("hammer_condition:")
    public int hammer_condition = 0;       
    //@Configurable("hammer_perc:")
    public double hammer_perc = 0.8;       
    @Configurable("sar_condition:")
    public int sar_condition = 1;       
    @Configurable("bollinger_condition:")
    public int bollinger_condition = 1;       
    @Configurable("adx_condition:")
    public int adx_condition = 1;       
    @Configurable("candlestick_condition:")
    public int candlestick_condition = 0;       
   
    
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

        double ema10 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 10, 0);   
        double ema25 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 25, 0);   
        double ema50 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 50, 0);
        double ema100 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 100, 0);   
        double ema200 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 200, 0);   

        double ema6_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 6, 1);   
        double ema12_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 1);   
        double ema34_y = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 34, 1);

        double adx = indicators.adx(selectedInstrument, selectedPeriod, OfferSide.BID, 14, 1);


        double sar = indicators.sar(selectedInstrument, selectedPeriod, OfferSide.BID, 0.02, 0.2, 0);


        double [] todayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 0);


        double [] fractals = indicators.fractalLines(instrument, period, OfferSide.BID, 5, 0);
        double [] fractals_y = indicators.fractalLines(instrument, period, OfferSide.BID, 5, 1);
        
        Object[] indicatorResult = indicators.calculateIndicator(selectedInstrument, selectedPeriod, new OfferSide[] {OfferSide.BID},
                "BBANDS", new IIndicators.AppliedPrice[] {IIndicators.AppliedPrice.CLOSE}, new Object[] {20, 2.0, 2.0, 0}, Filter.WEEKENDS, 1, time, 0);
        double bollingerUpperValue = ((double[]) indicatorResult[0])[0];
        double bollingerMiddleValue = ((double[]) indicatorResult[1])[0];
        double bollingerLowerValue = ((double[]) indicatorResult[2])[0];        
        
        
        double rsi = indicators.rsi(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, 0);        
        IBar bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 1);
        IBar bar_y = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 2);
        IBar bar_2y = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 3);

        boolean goLong = long_bool == 1;
        boolean goShort = short_bool == 1;
        
       
        
        
        if(adx_condition == 1)
        {
            goLong = goLong && adx > 20;
            goShort = goShort && adx > 20;
        }

        if(bollinger_condition == 1)
        {
            double candleLow = bar.getOpen() > bar.getClose() ? bar.getClose() : bar.getOpen();
            double candleHigh = bar.getOpen() < bar.getClose() ? bar.getClose() : bar.getOpen();
            
            goLong = goLong && candleLow > bollingerMiddleValue && candleHigh < bollingerUpperValue;
            goShort = goShort && candleLow > bollingerLowerValue && candleHigh < bollingerMiddleValue;
        }
                        
                                        
        if(sar_condition == 1)
        {
            goLong = goLong && bar.getLow() > sar;
            goShort = goShort && bar.getHigh() < sar;
        }
        
        if(hammer_condition == 1)
        {
            //goLong = goLong && bar_y.getClose() >= bar_y.getLow() + (bar_y.getHigh()-bar_y.getLow()) * hammer_condition;
            //goShort = goShort && bar_y.getClose() <= bar_y.getLow() + (bar_y.getHigh()-bar_y.getLow()) * (1.0-hammer_condition);

            goLong = goLong && (bar_y.getClose() == bar_y.getHigh() || bar.getLow() == bar.getOpen());
            goShort = goShort && (bar_y.getClose() == bar_y.getLow() || bar.getHigh() == bar.getOpen());

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
        }else if(ema_condition == 3)
        {
            goLong = goLong && ema6 > ema12 && ema12 > ema34 && bar.getClose() > ema6;
            goShort = goShort && ema6 < ema12 && ema12 < ema34 && bar.getClose() < ema6;
        }else if(ema_condition == 4)
        {
            goLong = goLong && bar.getClose() > ema10 && bar_y.getClose() < ema10;
            goShort = goShort && bar.getClose() < ema10 && bar_y.getClose() > ema10;
        }else if(ema_condition == 5)
        {
            goLong = goLong && bar.getClose() > ema25 && bar_y.getClose() < ema25;
            goShort = goShort && bar.getClose() < ema25 && bar_y.getClose() > ema25;
        }else if(ema_condition == 6)
        {
            goLong = goLong && bar.getClose() > ema50 && bar_y.getClose() < ema50;
            goShort = goShort && bar.getClose() < ema50 && bar_y.getClose() > ema50;
        }else if(ema_condition == 7)
        {
            goLong = goLong && bar.getClose() > ema100 && bar_y.getClose() < ema100;
            goShort = goShort && bar.getClose() < ema100 && bar_y.getClose() > ema100;
        }else if(ema_condition == 8)
        {
            goLong = goLong && bar.getClose() > ema200 && bar_y.getClose() < ema200;
            goShort = goShort && bar.getClose() < ema200 && bar_y.getClose() > ema200;
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
                for (IOrder order : engine.getOrders(selectedInstrument)) {
                    if (order.getState() == IOrder.State.OPENED && !order.isLong()) {
                        order.close();
                    }
                }            
                 
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

                 double tp = price + (price - sl) * 2;
                 if(takeProfitPips > 0)
                 {
                     tp = price + instrument.getPipValue() * takeProfitPips;
                 }
                 
                 
                 double risk = price - sl;
                 double howMuchCanIRisk = equity * risk_profile / 100;
                 double singleMicroLotsRisk = risk * 1000 / leverage;                 
                 int howManyMicroLots = (int)(singleMicroLotsRisk / howMuchCanIRisk);
                 //double quantity = (double)howManyMicroLots / 100.0;     
                 double quantity = getLot(price,sl);
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("BuyStopOrder", instrument, IEngine.OrderCommand.BUYSTOP, quantity, price, 20, sl, tp, gtt, "BUY");
                 
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
                 
                 double tp = price + (price - sl) * 2;
                 if(takeProfitPips > 0)
                 {
                     tp = price - instrument.getPipValue() * takeProfitPips;
                 }
                 
                 
                 double risk = price - sl;
                 double howMuchCanIRisk = equity * risk_profile / 100;
                 double singleMicroLotsRisk = risk * 1000 / leverage;                 
                 int howManyMicroLots = (int)(singleMicroLotsRisk / howMuchCanIRisk);
                 //double quantity = (double)howManyMicroLots / 100.0;     

                 double quantity = getLot(price,sl);
                 
                                                   
                 //double tp = price - instrument.getPipValue() * takeProfitPips;
                 long gtt = lastTick.getTime() + period.getInterval() * candlesToWait; //withdraw after 30 secs
                 IOrder order = engine.submitOrder("SellStopOrder", instrument, IEngine.OrderCommand.SELLSTOP, quantity, price, 20, sl, tp, gtt, "SELL"); 

                  console.getOut().println("SellStopOrder - Price: " + price + " SL: " + sl + " TP: " + tp);
        
        }
        
        
                      
               
        
    }
    
    private double getLot(double price, double stoploss) throws JFException
    {
        double lotSize = 0;

        double diff = price > stoploss ? price - stoploss : stoploss - price;

        equity = context.getAccount().getEquity();
        
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
    
    
}
