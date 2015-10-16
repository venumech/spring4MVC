package com.venu.develop.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

/**
 * Version:1
 * Legacy version.
 * Simple Java Program to connect Oracle database by using Oracle JDBC thin
 * driver. Make sure you have Oracle JDBC thin driver in your classpath before
 * running this program
 *  This Version uses a legacy style Oracle Stored Procedure
 *  (file: order_save_version1_sp.sql, SP name: ORDER_PROCESS_PROC_OLD )
 *   which employs 
 *  the dynamic sql execution (not an efficient way!) for inserting the data
 *   and standard SQL to look up the database for a given orderid.
 *   in the version2, the more efficient approach is employed, where the oracle collections
 *   and the custom object types are created
 * @author venu
 */

//@Repository("orderDao")
public class OrderDBHelperVersion1 implements OrderDBInfc {

	@Value("${oracle.db.driver}")
	private String oraDriver;

	@Value("${oracle.db.url}")
	private String oraUrl;

	@Value("${oracle.user}")
	private String oraUser;

	@Value("${oracle.password}")
	private String oraPwd;

	private final Logger logger = LoggerFactory.getLogger(OrderDBHelperVersion1.class);

	private static final String sqlLookUp = "Select a.id, instructions, b.city as from_city, b.state as from_state, b.zip as from_zip,"
			+ " c.city as to_city, c.state as to_state, c.zip as to_zip,"
			+ " weight, volume, hazard, product "
			+ " from ORDERS a, (select * from ADDRESS) b, (select * from ADDRESS) c, LINE_ITEMS d"
			+ " where a.id = d.order_id and a.from_address_id = b.address_id" + " and a.to_address_id = c.address_id"
			+ " and a.id = ";

	@Override
	public Order saveId(Order o) throws SQLException {
		Long id = dataInsertWork(o);
		o.setId(id);
		return o;

	}

	/*
	 * Dynamic sql generation. this is for inserting line items into databse as
	 * the number of line items for a each of the orders keep varies. There are
	 * many ways to do but quick way is here
	 */
	private String buildLineItemsSQL(List<LineItem> lineItems) {
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT ALL ");
		for (LineItem lineitem : lineItems) {
			Boolean bl = lineitem.getHazard();
			String hazard_str = "'N'";

			if (bl == null)
				hazard_str = "null";
			else if (bl) {
				hazard_str = "'Y'";
			}
			sql.append("INTO LINE_ITEMS (order_id , weight, volume, hazard, product ) ");
			sql.append("VALUES (:order_id_val, ");
			sql.append(lineitem.getWeight() + ", ");
			sql.append(lineitem.getVolume() + ", ");
			sql.append(hazard_str + ", ");
			sql.append("'" + lineitem.getProduct() + "') ");
		}
		sql.append("SELECT 1 FROM dual");

		System.out.println(sql);

		return sql.toString();
	}


	/*
	 * search database for a given order id
	 */
	public Order findById(Long orderId) throws SQLException {

		Order order = new Order();
		int rowCount = 0;

		order.setId(orderId);
		// URL of Oracle database server
		String url = oraUrl; // "jdbc:oracle:thin:@localhost:1521:LOGISTICS";

		// properties for creating connection to Oracle database
		Properties props = new Properties();
		props.setProperty("user", oraUser);
		props.setProperty("password", oraPwd);

		// creating connection to Oracle database using JDBC
		Connection conn = null;

		PreparedStatement pstmt =null;
		try {

			logger.debug(" load driver into classpath........................................");
			// load driver into classpath
			
			Class.forName(oraDriver);
			conn = DriverManager.getConnection(url, props);

			// creating PreparedStatement object to execute query
			String sqlStr = sqlLookUp + orderId;
			System.out.println(sqlStr);
			pstmt = conn.prepareStatement(sqlStr);
			logger.debug(" done the PreparedStatement and executing the query");

			
			Address fromAddress = new Address();
			Address toAddress = new Address();
			List<LineItem> lineitems = new ArrayList<LineItem>();
			ResultSet rs = pstmt.executeQuery();
			
			if (rs == null){
				logger.debug("rs == null ");
			
				String error= " The Order Id : " + orderId + " does not Exists in the System";
				logger.debug("ERROR: "+ error);
				throw new SQLException("ERROR: " + error);
			}
			
			
			String str = "";
			Double val = 0d;
			logger.debug(" Reading the resultSet");

			while (rs != null && rs.next()) {
				rowCount++;
				LineItem lineItem = new LineItem();
				str = rs.getString("instructions");
				order.setInstructions(str);
				str = rs.getString("from_city");
				fromAddress.setCity(str);

				str = rs.getString("from_state");
				fromAddress.setState(str);
				str = rs.getString("from_zip");
				fromAddress.setZip(str);
				str = rs.getString("to_city");
				toAddress.setCity(str);
				str = rs.getString("to_state");
				toAddress.setState(str);
				str = rs.getString("to_zip");
				toAddress.setZip(str);
				val = rs.getDouble("weight");
				lineItem.setWeight(val);
				val = rs.getDouble("volume");
				lineItem.setVolume(val);
				str = rs.getString("hazard");
				lineItem.setHazard(new Boolean(str));
				str = rs.getString("product");
				lineItem.setProduct(str);

				lineitems.add(lineItem);
			}

			logger.debug("Done. Reading the resultSet");

			order.setFrom(fromAddress);
			order.setTo(toAddress);
			order.setLines(lineitems);

			logger.debug("done. fetch the Order details: Order id = " + orderId);

			if (rowCount == 0) {
				logger.debug("row Fetch Size= " + rowCount);
				return null;
			}
		}  catch (SQLException | ClassNotFoundException e) {
			logger.error("ERROR: ", e.getMessage());
			throw new SQLException("ERROR: " + e.getMessage());
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}

		// logger.debug(order.toString());

		return order;

	}

	private Long dataInsertWork(Order order) throws SQLException {

		Long orderId = 0l;
		String message = "";

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

			// creating PreparedStatement object to execute query
			cstmt = conn.prepareCall("begin " + "venu.order_process_proc(:1,:2,:3,:4,:5,:6,:7,:8,:9, :10); end;");

			cstmt.registerOutParameter(9, Types.CHAR); // order id
			cstmt.registerOutParameter(10, Types.CHAR); // message

			cstmt.setString(1, order.getFrom().getCity());
			cstmt.setString(2, order.getFrom().getState());
			cstmt.setString(3, order.getFrom().getZip());
			cstmt.setString(4, order.getTo().getCity());
			cstmt.setString(5, order.getTo().getState());
			cstmt.setString(6, order.getTo().getZip());
			cstmt.setString(7, buildLineItemsSQL(order.getLines()));
			cstmt.setString(8, order.getInstructions());

			cstmt.execute();
			orderId = cstmt.getLong(9);
			message = cstmt.getString(10);
		} catch (SQLException | ClassNotFoundException e) {
			System.out.println("order not processed in database. " + message);
			System.out.println(e.getMessage());

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

		return orderId;

	}

}