package com.venu.develop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.springframework.web.multipart.MultipartFile;

import com.venu.develop.model.Order;

public interface OrderProcessService {


	public Order saveOrder(MultipartFile mFile) throws IOException, SQLException, XMLStreamException;
	
	public Order searchOrder (Long orderId) throws SQLException, IOException, ClassNotFoundException;

	public List<String> getAutoCompleteList(String orderId);
}
