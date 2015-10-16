package com.venu.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;

import com.venu.develop.conf.OrderProcessInitializer;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = OrderProcessInitializer.class)
@WebAppConfiguration
public class OrderProcessRestfulServiceApplicationTests {

	@Test
	public void contextLoads() {
	}

}
