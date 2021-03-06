package com.venu.develop.dao;

import java.io.IOException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.venu.develop.model.*;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import oracle.jdbc.OracleTypes;

import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import java.math.BigDecimal;
import oracle.sql.ARRAY;
import oracle.sql.STRUCT;

/**
 * Version:3
 * in this alternative version,  spring is employed for data base activity
  * Simple Java Program to connect Oracle database by using Oracle JDBC thin driver.
  * Make sure you have Oracle JDBC thin driver in your classpath before running this program.
  * This Version uses a classic style Oracle Stored Procedure making use of spring jdbc.
 *  (1. file: order_save_sp.sql, SP name: ORDER_PROCESS_PROC )
 *   which employs Oracle Collections and custom types  for inserting the data.
 *   To search the database for an order id the below SP is being used.
 *   (2. file: order_lookup.sql, SP name: ORDER_PROCESS_LOOKUP).
 *   
  * @author venu
  */

//@Repository("orderDao")
@Transactional
public class OrderDBHelperVersion3 extends JdbcDaoSupport  implements OrderDBInfc {

	private final Logger logger = LoggerFactory.getLogger(OrderDBHelperVersion3.class);
    
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DriverManagerDataSource dataSource;
    
	public OrderDBHelperVersion3(){}
	
	public OrderDBHelperVersion3(DataSource basicDataSource) {
		this.jdbcTemplate = new JdbcTemplate(basicDataSource);
	}


	@PostConstruct
	private void initialize() {
		// logger.info("Database server time is: {}", 
		//		jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP from dual", Date.class));
		setDataSource(dataSource);
	}
	
	
	private OrderLookUpProcedure getOrder;
	
	
	/*
	 * Inner class for Order saving
	 */
	private class SaveOrderStoredProcedure extends StoredProcedure {

        private static final String SPROC_NAME = "venu.order_process_proc";
   
        public SaveOrderStoredProcedure(DataSource dataSource) {
            super(dataSource, SPROC_NAME);
            
            //order of these params must match to the stored procedure params order.

            declareParameter(new SqlParameter("from_city", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("from_state", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("from_zip", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("to_city", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("to_state", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("to_zip", OracleTypes.VARCHAR));
            declareParameter(new SqlInOutParameter("dynamic_line_item_sql", OracleTypes.VARCHAR));
            declareParameter(new SqlParameter("instructions", OracleTypes.VARCHAR));
            declareParameter(new SqlOutParameter("l_order_id", OracleTypes.NUMBER));
            declareParameter(new SqlOutParameter("message", OracleTypes.VARCHAR));

            compile();
        }

 	   /*
 	    * Insert 'order' into the database and return the OrderId
 	    *  generated by oracle stored proc, if the database activity is successful
 	    */
        
        public Order saveOrder(Order order) throws SQLException {

            Map<String, Object> inParameters = new HashMap<String, Object>(8);

            //order of these params must match to the stored procedure params order.
            inParameters.put("from_city", order.getFrom().getCity());
            inParameters.put("from_state", order.getFrom().getState());
            inParameters.put("from_zip", order.getFrom().getZip());
            inParameters.put("to_city", order.getTo().getCity());
            inParameters.put("to_state", order.getTo().getState());
            inParameters.put("to_zip", order.getTo().getZip());
            inParameters.put("dynamic_line_item_sql", buildLineItemsSQL (order.getLines()));
            inParameters.put("instructions", order.getInstructions());

            Map<?, ?> outParameters = super.execute(inParameters);
        	
        	//1. get 'order id'
        	BigDecimal orderId = (BigDecimal) outParameters.get("l_order_id");

        	logger.debug("saved. orderId= " + orderId);
        	order.setId(orderId.longValue());
        	
        	//2. get 'status message from the Stored Proc'
        	String msgSP = (String) outParameters.get("message");
        	logger.debug("mesage from Stored Proc= " + msgSP);
        	
        	if (msgSP != null){
        		throw new SQLException(msgSP);
        	}
        	
            return order;
        }       		
	}
	
	/*
	 * Inner class for Order search
	 */
	private class OrderLookUpProcedure extends StoredProcedure {
		
        private static final String SPROC_NAME = "VENU.ORDER_PROCESS_LOOKUP";
        
        public OrderLookUpProcedure(DataSource dataSource) {
            super(dataSource, SPROC_NAME);
            //order of these params must match to the stored procedure params order.
            declareParameter(new SqlOutParameter("l_instructions", OracleTypes.VARCHAR));
            declareParameter(new SqlOutParameter("l_line_items", OracleTypes.ARRAY, "LINES_TABLE")); //LINES_TABLE --> Collection( or table) Oracle custom type, "LINEITEM_OBJECT"
            declareParameter(new SqlOutParameter("l_from_address", OracleTypes.STRUCT,"ADDRESS_OBJ")); //FromAddress: ADDRESS_OBJ --> Oracle custom type
            declareParameter(new SqlOutParameter("l_to_address", OracleTypes.STRUCT, "ADDRESS_OBJ"));//ToAddress: ADDRESS_OBJ --> Oracle custom type
            
            declareParameter(new SqlParameter("l_order_id", OracleTypes.INTEGER));
            declareParameter(new SqlInOutParameter("message", OracleTypes.VARCHAR));

            compile();
        }


 	   /*
 	    * search database for a given order_id and return Order
 	    */
        
        public Order retrieveOrder(Long orderId) throws SQLException {

            Map<String, Object> inParameters = new HashMap<String, Object>(2);
            inParameters.put("l_order_id", orderId);
           inParameters.put("message", "x");

            Map outParameters = super.execute(inParameters);


            	Order order = new Order();
            	
            	//1. get 'instructions'
            	String instructions = (String) outParameters.get("l_instructions");

            	logger.debug("instructions= " + instructions);
            	order.setInstructions(instructions);
            	
            	order.setId(orderId);
        		List<LineItem> lineitems = new ArrayList<LineItem>();

    			
    			//2. get 'From' address
            	STRUCT struct1 = (STRUCT) outParameters.get("l_from_address");
            	Object[] attr1 = struct1.getAttributes();
            	Address fromAddress = new Address((String)attr1[0], (String)attr1[1], (String)attr1[2]);
            	
            	order.setFrom(fromAddress);
            	//3. get 'To' address
            	STRUCT struct = (STRUCT) outParameters.get("l_to_address");
            	Object[] attr2 = struct.getAttributes();
            	Address toaddress = new Address((String)attr2[0], (String)attr2[1], (String)attr2[2]);

            	order.setFrom(fromAddress);

        		
            	//4. get Line_Items
            	ARRAY array = (ARRAY) outParameters.get("l_line_items");
    			Object[] rows = (Object[]) array.getArray();

    			for (Object row : rows) {
    				Object[] cols = ((oracle.sql.STRUCT) row).getAttributes();
    				LineItem lI = new LineItem();

    				lI = new LineItem(((BigDecimal) cols[0]).doubleValue(), ((BigDecimal) cols[1]).doubleValue(),
    						cols[2].equals("Y") ? true : false, cols[3].toString());
    				lineitems.add(lI);
    			}
            	
    			order.setLines(lineitems);
    			
    			order.setId(orderId);
            	return order;
        }        
        
        

		
	}
	
	private class OrderLookUpProcedureKup extends StoredProcedure {
		

        private static final String SPROC_NAME = "VENU.AGGR";

        
        public OrderLookUpProcedureKup(DataSource dataSource) {
            super(dataSource, SPROC_NAME);
            declareParameter(new SqlParameter("l_1", OracleTypes.INTEGER));
            declareParameter(new SqlInOutParameter("result", OracleTypes.INTEGER));

            compile();
        }

        public Integer aggregate(Integer start, Integer end) {
            Map<String, Object> inParameters = new HashMap<String, Object>(2);
            inParameters.put("l_1", 100);
            inParameters.put("result", 100);

            Map outParameters = super.execute(inParameters);
            if (outParameters.size() > 0) {
              return (Integer) outParameters.get("result");
            } else {
              return 0;
            }
          }

		
	}
	
	
	/////////////////////

	
	@Override
	public Order findById(Long orderId) throws IOException, SQLException, ClassNotFoundException {
		Order order = null;
		
		if (jdbcTemplate == null){
			System.out.println(" jdbcTemplate NULL +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++========" );
		}
		/*
		int returnval = jdbcTemplate.update("select 1 from dual");
		System.out.println("returnval ========" + returnval);
		*/
		order = new OrderLookUpProcedure(dataSource).retrieveOrder(orderId);
		return order;
	}

	@Override
	public Order saveId(Order order) throws IOException, SQLException {
		
		logger.debug("Executing the Stored Procedure, 'venu.order_process_proc' ...");
		
		order = new SaveOrderStoredProcedure(dataSource).saveOrder(order);

		logger.debug("done executing the Stored Procedure, 'venu.order_process_proc'. order_id = " + order.getId());
		
		return order;
	}
	
    /*
     * Dynamic sql generation. this is for inserting line items into databse as the number
     *  of line items for a each of the orders keep varies.
     * There are many ways to do but quick way is here
     * TODO replace with Oracle ARRAY later similar to the findById()
     */
    private String buildLineItemsSQL (List<LineItem> lineItems) {
    	StringBuilder sql = new StringBuilder();
    	sql.append ("INSERT ALL " );
    	for (LineItem lineitem: lineItems){ 
    		Boolean bl = lineitem.getHazard();
    		String hazard_str = "'N'";
    		 
    		if (bl ==null)
    			hazard_str = "null";
    		else if (bl){
    			hazard_str="'Y'";
    		}
        	sql.append ("INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) ");
    		sql.append ("VALUES (:order_id_val, ");	
    		sql.append(lineitem.getWeight() +", ");
    		sql.append(lineitem.getVolume() +", ");
    		sql.append(hazard_str +", ");
    		sql.append("'"+lineitem.getProduct() +"') ");    		
    	}
    	sql.append("SELECT 1 FROM dual");
    	
    	System.out.println(sql);
    		
    	return sql.toString();
    }

	
	

}
