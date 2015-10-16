package com.venu.test;
 
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.venu.develop.model.Order;

 
public class SpringRestTestClient {
	private static final Logger logger = LoggerFactory.getLogger(SpringRestTestClient.class);

    public static final String REST_SERVICE_URI = "http://localhost:9990/OrderProcessRestService";
     
    /* GET */
    @SuppressWarnings("unchecked")
    private static void listAllOrders(){
    	logger.debug("Testing listAllOrders API-----------");
         
        RestTemplate restTemplate = new RestTemplate();
        List<LinkedHashMap<String, Object>> ordersMap = restTemplate.getForObject(REST_SERVICE_URI+"/user/", List.class);
         
        if(ordersMap!=null){
            for(LinkedHashMap<String, Object> map : ordersMap){
            	logger.debug("Order : id="+map.get("id"));;
            }
        }else{
        	logger.debug("No order exist----------");
        }
    }
     
    /* GET */
    private static void searchOrder(){
    	logger.debug("Testing searchOrder API----------");
        RestTemplate restTemplate = new RestTemplate();
        Order order = restTemplate.getForObject(REST_SERVICE_URI+"/search/1", Order.class);
        logger.debug(order.getId().toString());
    }
     
 
    public static void main(String args[]){
        listAllOrders();
        searchOrder();
        listAllOrders();
    }
}