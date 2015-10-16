package com.venu.develop.dao;



import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.venu.develop.model.*;

import oracle.jdbc.OracleTypes;
import oracle.jdbc.OracleStruct;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import oracle.sql.TypeDescriptor;

/**
 * Version:2
  * Simple Java Program to connect Oracle database by using Oracle JDBC thin driver.
  * Make sure you have Oracle JDBC thin driver in your classpath before running this program.
  * This Version uses a classic style Oracle Stored Procedure
 *  (file: order_save_sp.sql, SP name: ORDER_PROCESS_PROC )
 *   which employs Oracle Collections and custom types  for inserting the data.
 *   To search the database for an order id the below SP is being used.
 *   file: order_lookup.sql, SP name: ORDER_PROCESS_LOOKUP.
 *   in the next improved version, 'version.3', data base activity using spring is employed 
  * @author venu
  */
@Repository("orderDao")
public class OrderDBHelperVersion2 implements OrderDBInfc{
	 
	@Value("${oracle.db.driver}")
	private String oraDriver;

	@Value("${oracle.db.url}")
	private String oraUrl;

	@Value("${oracle.user}")
	private String oraUser;
 
	@Value("${oracle.password}")
	private String oraPwd;
 
	private final Logger logger = LoggerFactory.getLogger(OrderDBHelperVersion2.class);

	public Order saveId(Order order) throws SQLException{


		Long orderId = 0l;
		String message = "";
		String ORACLE_ARRAY = "LINES_TABLE"; //ADDRESS_OBJ --> Collection of Oracle custom type
		String ORACLE_STRUCT = "ADDRESS_OBJ"; //ADDRESS_OBJ --> Oracle custom type
		
		StructDescriptor fromStructDescriptor = null; //from address 
		StructDescriptor toStructDescriptor = null; //TO Address
		StructDescriptor structDescriptor = null; //LineItem
		ArrayDescriptor arrayDescriptor = null; //LineItems: Array of 'structDescriptor'
		int iSize = order.getLines().size();
		// URL of Oracle database server
		String url = oraUrl; // "jdbc:oracle:thin:@localhost:1521:LOGISTICS";

		// properties for creating connection to Oracle database
		Properties props = new Properties();
		props.setProperty("user", oraUser);
		props.setProperty("password", oraPwd);

		// creating connection to Oracle database using JDBC
		Connection conn = null;
		CallableStatement cstmt = null;

		try {
			// load driver into classpath
			Class.forName(oraDriver);

			conn = DriverManager.getConnection(url, props);

			fromStructDescriptor = StructDescriptor.createDescriptor(ORACLE_STRUCT, conn.getMetaData().getConnection());
			toStructDescriptor = StructDescriptor.createDescriptor(ORACLE_STRUCT, conn.getMetaData().getConnection());

			structDescriptor = StructDescriptor.createDescriptor("LINEITEM_OBJECT", conn.getMetaData().getConnection()); //LINEITEM_OBJECT --> Oracle custom type
			arrayDescriptor = ArrayDescriptor.createDescriptor(ORACLE_ARRAY, conn.getMetaData().getConnection()); // creating
																													// PreparedStatement

			Object[] arrayObj = new Object[order.getLines().size()];
			Object[][] lineItemsObj = new Object[arrayObj.length][4];
			STRUCT[] linestruct = new STRUCT[arrayObj.length];
			
			// Structuring obj and arrays: LINE_ITEMS
			
			for (int i = 0; i < lineItemsObj.length; i++) {
				lineItemsObj[i][0] = order.getLines().get(i).getWeight();
				lineItemsObj[i][1] = order.getLines().get(i).getVolume();
				lineItemsObj[i][2] = order.getLines().get(i).getHazard()?'Y':'N';
				lineItemsObj[i][3] = order.getLines().get(i).getProduct();

				//arrayObj[i] = new STRUCT(structDescriptor, conn.getMetaData().getConnection(), lineItemsObj[i]);

				linestruct[i] = new STRUCT(structDescriptor, conn.getMetaData().getConnection(), lineItemsObj[i]); 
			}
			
			// Create ARRAY from array of STRUCTS
			ARRAY arr = new ARRAY(arrayDescriptor, conn.getMetaData().getConnection(), linestruct);

			// Build 'From Address' Struct
			Object[] fromAddressObj = new Object[3];
			fromAddressObj[0] = order.getFrom().getCity();
			fromAddressObj[1] = order.getFrom().getState();
			fromAddressObj[2] = order.getFrom().getZip();
			STRUCT fromStruct = new STRUCT(fromStructDescriptor, conn.getMetaData().getConnection(), fromAddressObj);

			// Build 'To Address' Struct
			Object[] toAddressObj = new Object[3];
			toAddressObj[0] = order.getTo().getCity();
			toAddressObj[1] = order.getTo().getState();
			toAddressObj[2] = order.getTo().getZip();
			STRUCT toStruct = new STRUCT(toStructDescriptor, conn.getMetaData().getConnection(), toAddressObj);

			cstmt = conn.prepareCall("begin " + "venu.order_process_proc(:1,:2,:3,:4,:5,:6); end;");

			cstmt.registerOutParameter(5, Types.CHAR); // order id
			cstmt.registerOutParameter(6, Types.CHAR); // message

			cstmt.setObject(1, fromStruct, Types.STRUCT);
			cstmt.setObject(2, toStruct, Types.STRUCT);
			cstmt.setObject(3, arr, Types.ARRAY);
			cstmt.setString(4, order.getInstructions());

			cstmt.execute();
			
			message = cstmt.getString(6); //get the message returned by the stored proc.
			
			if (message != null ) {
				logger.debug("'Order' error. " + message);
				throw new SQLException( message);				
			}			
			orderId = cstmt.getLong(5);
			order.setId(orderId);
		} catch (SQLException | ClassNotFoundException e) {
			logger.warn("order not processed in database. " + message);
			logger.error(e.getMessage());

			throw new SQLException("ERROR: " + e.getMessage());
		} finally {
			try {
				if (cstmt != null)
					cstmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
		}

		System.out.println("done: Order id = " + orderId);

		return order;

	}
    
    /*
	    * search database for a given order id
	    */
	@SuppressWarnings("deprecation")
	public Order findById(Long orderId) throws SQLException {

		logger.debug("Entered OrderDBHelperVersion2.lookUpDB()");
		Order order = new Order();
		String message = "";

		order.setId(orderId);
		// URL of Oracle database server
		String url = oraUrl; // "jdbc:oracle:thin:@localhost:1521:LOGISTICS";

		// properties for creating connection to Oracle database
		Properties props = new Properties();
		props.setProperty("user", oraUser);
		props.setProperty("password", oraPwd);


		// creating connection to Oracle database using JDBC
		Connection conn = null;

		// creating PreparedStatement object to execute query
		CallableStatement cstmt = null;

		Address fromAddress = new Address();
		Address toAddress = new Address();
		List<LineItem> lineitems = new ArrayList<LineItem>();

		try {
			//load driver into classpath
			Class.forName(oraDriver);
			
			conn = DriverManager.getConnection(url, props);
			logger.debug("DriverManager loaded." + oraUser);

			conn = DriverManager.getConnection(url, props);

			// creating PreparedStatement object to execute query
			cstmt = conn.prepareCall("begin " + "VENU.ORDER_PROCESS_LOOKUP(:1,:2,:3,:4,:5,:6); end;");

			logger.debug("Callable statement prepared.");
			cstmt.registerOutParameter(1, Types.VARCHAR); // instructions
	
			cstmt.registerOutParameter(2, OracleTypes.ARRAY, "LINES_TABLE"); //LINES_TABLE --> Collection( or table) Oracle custom type, "LINEITEM_OBJECT"
			cstmt.registerOutParameter(3, OracleTypes.STRUCT, "ADDRESS_OBJ"); //FromAddress: ADDRESS_OBJ --> Oracle custom type
			cstmt.registerOutParameter(4, OracleTypes.STRUCT, "ADDRESS_OBJ"); //ToAddress: ADDRESS_OBJ --> Oracle custom type
			cstmt.setLong(5, orderId);
			cstmt.registerOutParameter(6, Types.CHAR); // message

			cstmt.execute();
			

			 message = cstmt.getString(6);
			
			if (message != null ) {
				logger.debug("'Order' error. " + message);
				throw new SQLException( message);				
			}
			
			logger.debug("Extracting Line Items.");

			ARRAY array = (ARRAY) cstmt.getObject(2);
			Object[] rows = (Object[]) array.getArray();

			for (Object row : rows) {
				Object[] cols = ((oracle.sql.STRUCT) row).getAttributes();
				LineItem lI = new LineItem();

				lI = new LineItem(((BigDecimal) cols[0]).doubleValue(), ((BigDecimal) cols[1]).doubleValue(),
						cols[2].equals("Y") ? true : false, cols[3].toString());

				// for (Object col : cols) { System.out.print(col); }

				lineitems.add(lI);
			}

			ResultSetMetaData metaData = null;
			// TypeDescriptor td = (TypeDescriptor) cstmt.getObject(9);
			/*
			 * //Address st = (Address) cstmt.getObject(9); //TypeDescriptor td
			 * = TypeDescriptor.getTypeDescriptor("ADDRESS_OBJ",
			 * (OracleConnection) conn); TypeDescriptor td = (TypeDescriptor)
			 * cstmt.getObject(8); td =
			 * TypeDescriptor.getTypeDescriptor("MY_TABLE", (OracleConnection)
			 * conn); td.getTypeCode(); short typeCode =
			 * td.getInternalTypeCode(); if (typeCode ==
			 * TypeDescriptor.TYPECODE_JDBC_ARRAY) { // check if it's a
			 * transient type if (td.isTransient()) { AttributeDescriptor[]
			 * attributes = ((StructDescriptor) td).getAttributesDescriptor();
			 * for (int i = 0; i < attributes.length; i++)
			 * System.out.println(attributes[i].getAttributeName()); } else {
			 * System.out.println(td.getTypeName());
			 * System.out.println(td.getTypeCodeName()); } }
			 */

			logger.debug("Extracting source Address.");
			Object[] data = (Object[]) ((STRUCT) cstmt.getObject(3)).getAttributes();// getOracleAttributes();
			fromAddress = new Address(data[0].toString(), data[1].toString(), data[2].toString());

			logger.debug("Extracting destination Address.");

			data = (Object[]) ((STRUCT) cstmt.getObject(4)).getAttributes();// getOracleAttributes();
			toAddress = new Address(data[0].toString(), data[1].toString(), data[2].toString());
			/*
			 * for(Object tmp : data) { Struct row = (Struct) tmp ; //
			 * Attributes are index 1 based... int idx = 1;
			 * 
			 * for(Object attribute : row.getAttributes()) {
			 * System.out.println(metaData.getColumnName(idx) + " = " +
			 * attribute); ++idx; } System.out.println(tmp); }
			 */
			String instructions = cstmt.getString(1);

			order.setFrom(fromAddress);
			order.setTo(toAddress);
			order.setInstructions(instructions);
			order.setLines(lineitems);
			order.setId(orderId);
			/*
			 * oracle.sql.STRUCT tdz = (oracle.sql.STRUCT)cstmt.getObject(8);
			 * Object[] x = tdz.getAttributes(); LineItem lI = new LineItem();
			 * Double weighta= (double) (x[0]); lI.setWeight(weighta); //
			 * ntd.name = (String)x[1]; System.out.println(weighta); //
			 * System.out.println(ntd.name); cstmt.close();
			 * 
			 * td = (TypeDescriptor) cstmt.getObject(8); short typeCode =
			 * td.getInternalTypeCode(); logger.debug("typeCode=" + typeCode);
			 * if(typeCode == TypeDescriptor.TYPECODE_OBJECT) { // check if it's
			 * a transient type if(td.isTransient()) { AttributeDescriptor[]
			 * attributes = ((StructDescriptor)td).getAttributesDescriptor();
			 * for(int i=0; i<attributes.length; i++)
			 * System.out.println(attributes[i].getAttributeName()); } else {
			 * System.out.println(td.getTypeName()); } }
			 */


		} catch (SQLException | ClassNotFoundException e) {
			logger.error("ERROR: " , e.getMessage());
			logger.error("ERROR: " , message);
			throw new SQLException("ERROR: " + e.getMessage());
		} finally {
			if (cstmt != null)
				cstmt.close();
			if (conn != null)
				conn.close();
		}

		logger.debug("done: found Order id = " + orderId);

		// logger.debug(order.toString());

		return order;

	}


}