package com.portfolens.config;

import com.portfolens.entity.Stock;
import com.portfolens.repository.StockRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockDataInitializer implements ApplicationRunner {
    private final StockRepository stocks;
    public StockDataInitializer(StockRepository stocks) { this.stocks=stocks; }
    @Override @Transactional public void run(ApplicationArguments args) {
        List<Stock> samples=List.of(
            new Stock("RELIANCE","Reliance Industries Limited","500325","Energy","Oil, Gas & Conglomerates"),
            new Stock("TCS","Tata Consultancy Services Limited","532540","Information Technology","IT Services"),
            new Stock("HDFCBANK","HDFC Bank Limited","500180","Financial Services","Banks"),
            new Stock("INFY","Infosys Limited","500209","Information Technology","IT Services"),
            new Stock("ITC","ITC Limited","500875","Consumer Goods","FMCG"),
            new Stock("ICICIBANK","ICICI Bank Limited","532174","Financial Services","Banks"),
            new Stock("SBIN","State Bank of India","500112","Financial Services","Banks"),
            new Stock("BHARTIARTL","Bharti Airtel Limited","532454","Telecommunications","Telecom Services"),
            new Stock("LT","Larsen & Toubro Limited","500510","Industrials","Engineering & Construction"),
            new Stock("WIPRO","Wipro Limited","507685","Information Technology","IT Services")
        );
        for (Stock stock : samples) if (!stocks.existsBySymbol(stock.getSymbol())) stocks.save(stock);
    }
}
