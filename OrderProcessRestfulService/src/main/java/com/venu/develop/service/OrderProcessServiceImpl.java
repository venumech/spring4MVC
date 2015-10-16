package com.venu.develop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.venu.develop.dao.OrderDBInfc;
import com.venu.develop.model.Order;

/**
 * service Layer
 * @author Venu
 * 
 */
@Service 
public class OrderProcessServiceImpl implements OrderProcessService {

	private final Logger logger = LoggerFactory.getLogger(OrderProcessServiceImpl.class);
	
	@Autowired
	private OrderParser orderParser;

	@Autowired
	private OrderDBInfc orderDao;

	@Autowired
	private LocalCacheService<Long, Order> lruCache;

	@Value("${ui.autocomplete.items}")
	private int autoCompleteSize;

	public OrderProcessServiceImpl() {
	}

	/* Save the order into the database.
	 * 
	 */
	@Override
	public Order saveOrder(MultipartFile mFile) throws IOException, SQLException, XMLStreamException {
		
		Order order = orderParser.parse(mFile);

		order = orderDao.saveId(order);
		lruCache.put(order.getId(), order);
		logger.debug("the order object is added to server side local cache! cache.size= " + lruCache.size());
		return order;
		
	}

	/*get the Order object from the server cache if it exists.
	  if not exists in the cache, then get it from the database over network connection
	  */
	@Override
	public Order searchOrder(Long orderId) throws IOException, SQLException, ClassNotFoundException {
		logger.debug("in OrderProcessServiceImpl.searchOrder( id:" + orderId + ")");

		Order order = new Order();
		

		order = lruCache.get(orderId); 
		
		if (order != null) {
			logger.debug("the 'Order' object is available in cache. no need to fetch from database.");

		} else {
			logger.debug("the order object is not in cache! Now, attepting to fetch from database.");
			order = orderDao.findById(orderId);
			
			//update the order info into the cache for future lookups
			lruCache.put(order.getId(), order); 
		}

		logger.debug("ids --> ");
		lruCache.printItems();
		logger.debug("Done. OrderProcessServiceImpl.searchOrder( id:" + orderId + ")");
		return order;
	}
	

	/*
	 * Process and return all the matching ids. the list is sorted and should
	 * not exceed the limit as mentioned in the 'application.properties'
	 */
	@Override
	public List<String> getAutoCompleteList(String word) {
		List<String> matchedIds = new ArrayList<String>();

		//get all the cache object keys(this is nothing but order ids)
		List<Long> orderIdList = lruCache.getAll();

		// displaying the set data
		logger.debug("update the json list which is used in the UI for autocomplete ");
		ListIterator<Long> iter = orderIdList.listIterator(orderIdList.size());

		//update the json list which will be used in the UI for autocomplete.
		//This list contains only the latest search orders.
		while (iter.hasPrevious() && matchedIds.size() < autoCompleteSize) {
			Long i1 = iter.previous();
			if (i1.toString().startsWith(word))
				matchedIds.add(i1.toString());
		}
		
		Collections.sort(matchedIds); //sort in ascending order.

		return matchedIds;
	}

}