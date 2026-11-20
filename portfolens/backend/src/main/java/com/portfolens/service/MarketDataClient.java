package com.portfolens.service;
import com.portfolens.dto.MarketDtos.PythonHistory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class MarketDataClient {
    private final RestClient client;
    public MarketDataClient(RestClient marketDataRestClient) { this.client=marketDataRestClient; }
    public PythonHistory history(String symbol, int offset, int limit) {
        return client.get().uri(uri -> uri.path("/api/market/stocks/{symbol}/history").queryParam("offset",offset).queryParam("limit",limit).build(symbol)).retrieve().body(PythonHistory.class);
    }
    public PythonHistory history(String symbol) {
        var first=history(symbol,0,1000);
        if(first==null||first.total_records()<=first.records().size()) return first;
        var records=new java.util.ArrayList<>(first.records());
        int offset=records.size();
        while(offset<first.total_records()) { var page=history(symbol,offset,1000); if(page==null||page.records()==null||page.records().isEmpty()) break; records.addAll(page.records()); offset+=page.records().size(); }
        return new PythonHistory(first.symbol(),first.company_name(),first.yahoo_ticker(),first.exchange(),first.data_start_date(),first.data_end_date(),first.last_updated(),first.source(),first.price_policy(),first.total_records(),0,records.size(),records);
    }
    public String stocks() { return client.get().uri("/api/market/stocks").retrieve().body(String.class); }
    public String status() { return client.get().uri("/api/market/data/status").retrieve().body(String.class); }
    public String download(String symbol) { return client.post().uri("/api/market/download/{symbol}",symbol).body(java.util.Map.of("period","1y","refresh",true)).retrieve().body(String.class); }
}
