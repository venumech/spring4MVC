/**
 * 
 */
package com.venu.develop.dao;

import java.io.IOException;
import java.sql.SQLException;

import com.venu.develop.model.Order;

/**
 * @author Venu
 *
 */

public interface OrderDBInfc {
	public Order findById(Long orderId) throws IOException, SQLException, ClassNotFoundException;
	public Order saveId(Order order) throws IOException, SQLException;
	String SERIALIZED_DATA_LOCATION = "resources/data/";
	String ORDER_XSD_LOCATION = "src/main/resources";
}
