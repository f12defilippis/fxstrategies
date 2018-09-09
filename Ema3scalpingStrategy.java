package jforex;

import com.dukascopy.api.*;
import java.util.*;

public class Ema3scalpingStrategy implements IStrategy {
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
    @Configurable("entryPips:")
    public int entryPips = 5;
    @Configurable("stopLossPips:")
    public int stopLossPips = 15;
    @Configurable("takeProfitPips:")
    public int takeProfitPips = 20;    
    
    
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
        
        double ema10 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 10, 0);   
        double ema21 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 21, 0);   
        double ema50 = indicators.ema(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 50, 0);
        double rsi = indicators.rsi(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 14, 0);
        
        double [] todayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 0);
        double [] yesterdayMACD = indicators.macd(selectedInstrument, selectedPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 12, 26, 9, 1);
        
        
        boolean macdBuy = yesterdayMACD[0] < yesterdayMACD[1] && todayMACD[0] > todayMACD[1];
        boolean macdSell = yesterdayMACD[0] > yesterdayMACD[1] && todayMACD[0] < todayMACD[1];
        
        
        console.getOut().println("Called");
        IBar bar = history.getBar(selectedInstrument, selectedPeriod, OfferSide.BID, 0);
        if(ema10 > ema21 && ema21 > ema50)
        {
            double entryprice = ema10 - ((ema10 - ema21)/2);
            double lowoc = bar.getClose() > bar.getOpen() ? bar.getOpen() : bar.getClose();
            
//            if(lowoc < entryprice && rsi < 70 && macdBuy)
            if(rsi < 70 && macdBuy)
            {
                 ITick lastTick = history.getLastTick(instrument);
                 double price = lastTick.getAsk() + instrument.getPipValue() * entryPips;
                 double sl = price - instrument.getPipValue() * stopLossPips;
                 double tp = price + instrument.getPipValue() * takeProfitPips;
                 //long gtt = lastTick.getTime() + TimeUnit.SECONDS.toMillis(30); //withdraw after 30 secs
                 IOrder order = engine.submitOrder("BuyStopOrder", instrument, IEngine.OrderCommand.BUYSTOP, 0.01, price, 20, sl, tp, 0, "BUY"); 
                 console.getOut().println("Order entered");
            }
            
        }else if(ema50 > ema21 && ema21 > ema10)
        {
            double entryprice = ema21 + ((ema10 - ema21)/2);
            double highoc = bar.getClose() < bar.getOpen() ? bar.getOpen() : bar.getClose();
            
//            if(highoc > entryprice && rsi > 30)
            if(rsi > 30 && macdSell)
            {
                 ITick lastTick = history.getLastTick(instrument);
                 double price = lastTick.getAsk() - instrument.getPipValue() * 5;
                 double sl = price + instrument.getPipValue() * 10;
                 double tp = price - instrument.getPipValue() * 15;
                 //long gtt = lastTick.getTime() + TimeUnit.SECONDS.toMillis(30); //withdraw after 30 secs
                 IOrder order = engine.submitOrder("SellStopOrder", instrument, IEngine.OrderCommand.SELLSTOP, 0.01, price, 20, sl, tp, 0, "SELL"); 
                 console.getOut().println("Order entered");
            }
            
        }
        
        
                      
               
        
    }
}