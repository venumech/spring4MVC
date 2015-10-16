package com.venu.develop.controller;
 
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.venu.develop.model.Order;
import com.venu.develop.model.OrderError;
import com.venu.develop.service.OrderProcessService;
@RestController
public class OrderProcessRestController {
 
	@Autowired
	private  OrderProcessService orderProcessServiceImpl;
    

    /*
     * Search Order activity
     */    
    @RequestMapping(value = "/search/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> searchOrder(@PathVariable("id") long q) {
        System.out.println("Fetching User with id " + q);
        Order order=null;
		OrderError oError = new OrderError();
		try {
			order = orderProcessServiceImpl.searchOrder(q);
		} catch (ClassNotFoundException | SQLException | IOException e) {
			e.printStackTrace();
			setErrorObj(oError, e.getMessage());
		}
        if (oError.getError() || order == null) {
            System.out.println("Order with id " + q + " not found");

            return new ResponseEntity<Order>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }
 
    /*
     * autocomplete feature added.
     * This is to send the matched order ids to the browser client 
     */
    
	@RequestMapping(value = "/getMatchedIds.do", method = RequestMethod.GET)
	public @ResponseBody List<String> getMachedNames(@RequestParam("term") String query) {

		List<String> matchedIds = orderProcessServiceImpl.getAutoCompleteList(query);

		return matchedIds;
	}

    	private OrderError setErrorObj(OrderError oError, String errorMsg){
    		
    		oError.setError(true);
    		oError.setErrorMsg(errorMsg);
    		return oError;
    	}
 
}