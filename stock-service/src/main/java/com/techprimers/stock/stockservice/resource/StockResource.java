package com.techprimers.stock.stockservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/stock")
public class StockResource {

	@Autowired
	RestTemplate restTemplate;

	 @HystrixCommand(fallbackMethod = "fallbackStock" )
	@GetMapping("/v1/{username}")
	public List<Stock> getStockV1(@PathVariable("username") final String userName) {

		ResponseEntity<List<String>> quoteResponse = restTemplate.exchange("http://db-service/rest/db/" + userName,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {
				});

		List<String> quotes = quoteResponse.getBody();
		return quotes.stream().map(this::getStockPrice).collect(Collectors.toList());
	}

	 @HystrixCommand(fallbackMethod = "fallbackQuote" )
	@GetMapping("/{username}")
	public List<Quote> getStock(@PathVariable("username") final String userName) {

		ResponseEntity<List<String>> quoteResponse = restTemplate.exchange("http://db-service/rest/db/" + userName,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {
				});

		List<String> quotes = quoteResponse.getBody();
		return quotes.stream().map(quote -> {
			Stock stock = getStockPrice(quote);
			System.out.println("This is stock price for "+quote+" "+stock.getQuote().getPrice());
			return new Quote(quote, stock.getQuote().getPrice());
		}).collect(Collectors.toList());
	}
	 
	 public List<Stock> fallbackStock( final String userName, Throwable hystrixCommand) {
		 System.out.println("Called empty list");
	        return new LinkedList<Stock>();
	    }
	 
	 public List<Quote> fallbackQuote( final String userName, Throwable hystrixCommand) {
		 System.out.println("Called next one empty class");
	        return new LinkedList<Quote>();
	    }


	private Stock getStockPrice(String quote) {
		try {
			return YahooFinance.get(quote);
		} catch (IOException e) {
			e.printStackTrace();
			return new Stock(quote);
		}
	}

	private class Quote {
		private String quote;
		private BigDecimal price;

		public Quote(String quote, BigDecimal price) {
			this.quote = quote;
			this.price = price;
		}

		public String getQuote() {
			return quote;
		}

		public void setQuote(String quote) {
			this.quote = quote;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}
	}
}
