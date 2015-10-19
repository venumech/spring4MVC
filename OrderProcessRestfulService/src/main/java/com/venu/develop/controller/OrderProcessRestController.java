package com.venu.develop.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.venu.develop.model.Order;
import com.venu.develop.model.OrderError;
import com.venu.develop.service.OrderProcessService;

/**
 * need to build a simple RIA (Rich Internet Application) that allows for uploading and 
 * looking up Transportation Orders. The front end of the RIA would consist of a 
 * single HTML (JSP) page containing a form whose layout would slightly change depending 
 * on the chosen action:
 * 'Create Order’ action uploads the Transportation Order XML to the server, 
 * whereas ‘Search Order’ action looks up the order by its assigned ID.
 *
 * RestFul webservice implementation
 * @author Venu
 *
 */

@RestController
public class OrderProcessRestController {

	private final Logger logger = LoggerFactory.getLogger(OrderProcessRestController.class);

	
	@Autowired
	private OrderProcessService orderProcessServiceImpl;


	/*
	 * create order  handler.
	 * It takes the xml file and validates it and then saves the data to the database
	 */
	  @RequestMapping(value = "/createOrder/", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<Order> createOrder(@RequestParam("file") MultipartFile mFile) {

			logger.debug("createOrder() is started!");
			Order order = null;
			OrderError oError = new OrderError();
	        try {
	            if (mFile != null && mFile.getSize() > 0) {
	                order = orderProcessServiceImpl.saveOrder(mFile);
	            } else {
	    			setErrorObj(oError, "No file uploaded! ");
	            }
	        } catch (Exception e) {
	        	String status = "Error occurred while processing order, please try later." +e.getMessage();
        		setErrorObj(oError, status);
	        }
	        
	        if (oError.getError()) {
	        	logger.error("Error : "+ new Gson().toJson(oError));
	        	return  new ResponseEntity<Order>(HttpStatus.NOT_FOUND);
	        } else {
	            //logger.debug(  new Gson().toJson(order) ); //success
	        	logger.debug(  "Successfully created the order! " + order.getId()  ); //success
	        }	        
	        
			return new ResponseEntity<Order>(order, HttpStatus.OK);
	    }

	
	/*
	 * Search Order handler
	 */
	@RequestMapping(value = "/search/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Order> searchOrder(@PathVariable("id") long q) {
		System.out.println("Fetching User with id " + q);
		Order order = null;
		OrderError oError = new OrderError();
		try {
			order = orderProcessServiceImpl.searchOrder(q);
		} catch (ClassNotFoundException | SQLException | IOException e) {
			e.printStackTrace();
			setErrorObj(oError, e.getMessage());
		}

		if (oError.getError() || order == null) {
			System.out.println("Order with id, '" + q + "' not found. " + oError.getErrorMsg());
			return new ResponseEntity<Order>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Order>(order, HttpStatus.OK);
	}

	/*
	 * Auto complete feature. 
	 * This is to send the matched order ids to the client
	 */

	@RequestMapping(value = "/findMatchedIds.do", method = RequestMethod.GET)
	public @ResponseBody List<String> findMachedNames(@RequestParam("term") String query) {

		List<String> matchedIds = orderProcessServiceImpl.getAutoCompleteList(query);

		return matchedIds;
	}

	private OrderError setErrorObj(OrderError oError, String errorMsg) {

		oError.setError(true);
		oError.setErrorMsg(errorMsg);
		return oError;
	}

}